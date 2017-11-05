package com.spacetimeinsight.example.bbgdemo;

/**
 * Sample launcher
 */
public class Launcher {

    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0) {
            String option= args[0];
            String[] args2=new String[0];

            if( args.length > 1 ){
                args2= new String[args.length-1];
                System.arraycopy(args, 1, args2, 0, args2.length);
            }
            else {
                System.out.println("Usage: java -jar \"jarfile\" [\"bbg\" | \"console\"]");
            }

            if(option.equals("bbg")) {
                com.spacetimeinsight.example.bbgdemo.BeagleBone.main(args2);
            }
            else if(option.equals("console")) {
                com.spacetimeinsight.example.bbgdemo.Console.main(args2);
            }
        }
    }
}
