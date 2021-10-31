package com.mycompany.projektserver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

/**
 *
 * @author Kammila Przybysz
 */
public class WisielecServer {
    
   private static final int PORT = 9001;
   private static HashSet<String> names = new HashSet<String>();
   private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
   
   private static LinkedList<Pair> pary = new LinkedList<>();
   
   private static LinkedList<String> hasla = new LinkedList<String>();
   private static Random generator = new Random();
    
    public static void main(String[] args) throws Exception{
        System.out.println("Wisielec start...");
        ServerSocket listener = new ServerSocket(PORT);
        wczytanieHasel();
        System.out.println("ilosc haseł: "+hasla.size());
        try{
            while(true){
                new Handler(listener.accept()).start();
            }
        }finally{
            listener.close();
        }
    }
    
    public static int losujLiczbe(){
        return generator.nextInt(hasla.size());   
    }
    
    public static String losujHaslo(){
        int index = losujLiczbe();
        return hasla.get(index);
    }
    
    public static void losowanieNowegoHasla(Pair para){
        if(para.isNoweHaslo()){
            para.setProba(0);
            String str = losujHaslo();
            para.setAktualneHaslo(str);
            para.setUkryteHaslo(ukrywanieHasla(str));
            para.setNoweHaslo(false);
            para.getP1().println("nowe haslo "+para.getUkryteHaslo());
            para.getP2().println("nowe haslo "+para.getUkryteHaslo());
            para.getP1().println("message  nowa gra");
            para.getP2().println("message  nowa gra");
        }
    }
    
    public static String ukrywanieHasla(String haslo){
        char[] tab = haslo.toCharArray();
        StringBuilder builder = new StringBuilder();
        int liczba=0;
        
        for(char c:tab){
            liczba = (int)c;
            //jesli przedział a-z lub polskie w ascii
            if(((liczba>=97) && (liczba<=122)) 
                    || liczba==261 || liczba==263 || liczba==281 || liczba==322 || liczba==324 
                    || liczba==243 || liczba==347 || liczba==378 || liczba==380){
                builder.append("_");
            }else{
                builder.append(" ");
            }
        }
        return builder.toString();
    }
    
    public static void wczytanieHasel(){
        try {
            BufferedReader bf = new BufferedReader(new FileReader("hasla.txt"));
            String str = null;
            while((str = bf.readLine()) != null){
                hasla.add(str.trim().toLowerCase());
            }
            bf.close();
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    public static void parowanie(Handler h){
        boolean sparowane = false;
        boolean contains = false;
        while(true){
            if(pary.isEmpty()){
                pary.add(new Pair(h.out, null));
            }
            
            synchronized(pary){
                for(Pair p: pary){
                    PrintWriter p1 = p.getP1();
                    PrintWriter p2 = p.getP2();

                    if(p1 == h.out){
                        contains = true;
                        if(p2 != null){
                            h.opponent = p2;
                            sparowane = true;
                        }
                    }else if(p2 == h.out){
                        contains = true;
                        if(p1 != null){
                            h.opponent = p1;
                            sparowane = true;
                        }
                    }
                    if(sparowane)
                        break;
                }

                if(sparowane)
                    break;

                if(!contains){
                    for(Pair p: pary){
                        PrintWriter p1 = p.getP1();
                        PrintWriter p2 = p.getP2();

                        if(p1 == null){
                           contains = true;
                           p.setP1(h.out);
                        }else if(p2 == null){
                           contains = true;
                           p.setP2(h.out);
                        }
                        if(contains)
                            break;  
                        
                    }
                    if(!contains){
                        pary.add(new Pair(h.out, null));
                    }
                }
            }
        }
    }
    
    public static Pair przypisaniePary(Handler h){
        Pair przypisanie = null;
        for(Pair p:pary){
            if(p.getP1() == h.out && p.getP2() == h.opponent)
                przypisanie = p;
            else if(p.getP1() == h.opponent && p.getP2() == h.out)
                przypisanie = p;
        }
        return przypisanie;
    }
    
    public static String odgadywanieZnakow(String aktualneHaslo, String ukryteHaslo, char znak){
        char[] tabUkryte = ukryteHaslo.toCharArray();
        char[] tabAktualne = aktualneHaslo.toCharArray();
        int dlugoscTabUkryte = tabUkryte.length;
        StringBuilder builder = new StringBuilder();

        for(int i=0; i<dlugoscTabUkryte; i++){
            if(tabUkryte[i]=='_'){
                if(tabAktualne[i] == znak){
                    builder.append(znak);
                }else{
                    builder.append(tabUkryte[i]);
                }
            }else{
                builder.append(tabUkryte[i]);
            }
        }
        return builder.toString();
    }
    
    public static void aktywacjaTextField(Pair para){
        synchronized(para){
            if(para.isTurnP1()){
                para.getP1().println("moja tura");
                para.getP2().println("czekaj");
            }else{
                para.getP1().println("czekaj");
                para.getP2().println("moja tura");
            }
        }
    }
    
    public static boolean sprawdzenieCzyPrzegrana(Pair para){
        if(para.getProba() > 11){
            para.getP1().println("wisielec " + para.getProba());
            para.getP1().println("message  przegrales");
            para.getP2().println("wisielec " + para.getProba());
            para.getP2().println("message  przegrales");
            para.setNoweHaslo(true);
            return true;
        }
        return false;
    }
    
    private static class Handler extends Thread{
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private PrintWriter opponent;
        
        private Pair para;
        
        public Handler(Socket socket){
            this.socket = socket;
        }
        
        public void run(){
            try{
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                while(true){
                    out.println("name?");
                    name = in.readLine();
                    if(name == null){
                        return;
                    }
                    synchronized(names){
                        if(!names.contains(name)){
                            names.add(name);
                            break;
                        }
                    }
                }
                writers.add(out);
                
                out.println("waiting for opponent");
                parowanie(this);
                out.println("accepted");
                para = przypisaniePary(this);
                out.println("message  start");
                
                boolean isOk;
                while(true){
                    //sprawdzenie czy przeciwnik nie opuścił gry
                    isOk = true;
                    synchronized(pary){
                        if(para.getP1() != null && para.getP2() != null)
                            isOk=true;
                        else
                            isOk=false;
                    }
                    if(!isOk){
                        out.println("waiting for opponent");
                        parowanie(this);
                        opponent.println("accepted");
                        out.println("accepted");
                        opponent.println("message  start");
                        out.println("message  start");
                    }
                    opponent.println("accepted");
                    out.println("accepted");
                    
                    synchronized(para){
                        losowanieNowegoHasla(para);
                        aktywacjaTextField(para);
                        out.println("wisielec " + para.getProba());
                        opponent.println("wisielec " + para.getProba());
                    }
                    
                    String input = in.readLine();
                              
                    if (input != null){
                        input = input.toLowerCase().trim();
                            
                        if(input.isEmpty()){
                        }else if(input.length()==1){
                            out.println("message "+name+": "+input);
                            opponent.println("message "+name+": "+input);
                            String aktualneHaslo = para.getAktualneHaslo();
                            boolean containsChar = aktualneHaslo.contains(input);
                            if(containsChar){
                                String noweUkryte = odgadywanieZnakow(para.getAktualneHaslo(), para.getUkryteHaslo(), input.charAt(0));
                                para.setUkryteHaslo(noweUkryte);
                            }else{
                                para.setProba(para.getProba()+1);
                                sprawdzenieCzyPrzegrana(para);
                            }
                            synchronized(para){
                                para.getP1().println("aktualizacja ukrytego hasla "+ para.getUkryteHaslo());
                                para.getP2().println("aktualizacja ukrytego hasla "+ para.getUkryteHaslo());
                                para.zamianaTur();
                            }
                        }else{
                            out.println("message "+name+": "+input);
                            opponent.println("message "+name+": "+input);
                            synchronized(para){
                                if(input.equalsIgnoreCase(para.getAktualneHaslo())){    
                                    out.println("message  wygrales");
                                    opponent.println("message  przegrales");
                                    para.setNoweHaslo(true);
                                }else{
                                    para.setProba(para.getProba()+1);
                                    sprawdzenieCzyPrzegrana(para);
                                }
                                para.zamianaTur();
                             }
                        }
                    }
                }
            }catch (IOException e){
                
            }finally{
                if(name!=null){
                    names.remove(name);
                }
                if(out != null){
                    synchronized(pary){
                        opponent.println("opponent exit");
                        for(Pair p: pary){
                            PrintWriter p1 = p.getP1();
                            PrintWriter p2 = p.getP2();
                            if((p1 != null && p1.equals(out)) || (p2 != null && p2.equals(out))){
                                if(p1 != null && p1.equals(out)){
                                    p.setP1(null);
                                }else if(p2 != null && p2.equals(out)){
                                    p.setP2(null);
                                }
                                p.setNoweHaslo(true);
                                p.setAktualneHaslo("");
                                p.setUkryteHaslo("");
                            }
                        }
                        writers.remove(out);
                    }
                }
                try{
                    socket.close();
                }catch(IOException e){
                    
                }
            }
        }
    }
}
