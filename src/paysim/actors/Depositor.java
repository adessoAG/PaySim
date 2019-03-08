package paysim.actors;

import java.io.IOException;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.*;
//import org.apache.poi.xssf.usermodel.*;

public class Depositor {

        //getter und setter erst einmal nicht notwendig, da einige Variablen über den Konstruktor belegt und randomisiert werden.
        private int account_nr;
        private char Bank;
        private char location;
        private boolean payee;
        private boolean owner;
        private char group_of_customers;

        /*
                Später beim Mergen, müssen die Informationen für die jeweiligen Attribute aus ClientProfile etc entnommen werden und randomisiert werden zB location muss zufällig ausgewählt werden.
                eventuell eine eigene Funktion für eine zufällige Auswahl einer Location schreiben. Die Funktion soll sich erst einmal nur auf Deutschland fokussieren.
         */

        public Depositor(int acc_nr, char b, char l, boolean p, boolean o, char goc){

                    this.account_nr= acc_nr;
                    this.Bank= b;
                    this.location=l;
                    this.payee = p;
                    this.owner = o;
                    this.group_of_customers= goc;

        }

        //leerer Konstruktor.
        public Depositor(){}

        //Methode zum Einlesen der Objekte dieser Gruppe aus einer xlsx-Datei
        public void get_attributes_from_xlsx(){

        }

            /*
        //Methode zum Auslesen aller bestehenden Objekte in einer xlsx-Datei
        public ArrayList<String> read_all_objects_to_xlsx(String file_name, int pos) throws IOException {

            ArrayList<String> abc = new ArrayList<String>();
            Workbook workbook = new Workbook();
            FileInputStream fstream = new FileInputStream(file_name);
            workbook.open(fstream, FileFormatType.EXCEL97TO2003);

        }

    */

}
