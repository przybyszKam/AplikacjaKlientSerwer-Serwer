package com.mycompany.projektserver;

import java.io.PrintWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Kamila Przybysz
 */
@NoArgsConstructor
@Data
public class Pair {
    private PrintWriter p1;
    private PrintWriter p2;
    
    private String aktualneHaslo;
    private String ukryteHaslo;
    private boolean noweHaslo = true;
    
    private boolean turnP1 = true;//zaczyna gracz P1
    private boolean turnP2 = false;
    
    private int proba;
    
    public Pair(PrintWriter p1, PrintWriter p2){
        this.p1=p1;
        this.p2=p2;
    }
    
    public void zamianaTur(){
        if(turnP1){
            turnP1 = false;
            turnP2 = true;
        }else{
            turnP1 = true;
            turnP2 = false;
        }
    }

}
