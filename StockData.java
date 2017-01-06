// Josh Reavis
// Created for CSCI 330 Database Systems
// Fall 2016
// Provides the StockData class to conveniently provide a place to store our Stock Date

import java.io.*;


public class StockData{
   String Split;
   String Date;
   Double Open;
   Double Close;
   Double High;
   Double Low;
   Double Avg;
   Double OrgClose;
   Double OrgOpen;
   
   public StockData(String split, String date, Double open, Double high, Double low, Double close, Double avg, Double orgclose, Double orgopen){
      this.Split = split;
      this.Date = date;
      this.Open = open;
      this.Close = close;
      this.High = high;
      this.Low = low;
      this.Avg = avg;
      this.OrgClose = orgclose;
      this.OrgOpen = orgopen;

   }
   
   public void setAvg(Double avg){
      this.Avg = avg;
   }
   
   public String getSplit(){
      return this.Split;
   }
   
   public String getDate(){
      return this.Date;
   }
   
   public Double getOpen(){
      return this.Open;
   }
   
   public Double getClose(){
      return this.Close;
   }
   
   public Double getLow(){
      return this.Low;
   }
   
   public Double getHigh(){
      return this.High;
   }
   
   public Double getAvg(){
      return this.Avg;
   }
   
   public Double getOrgClose(){
      return this.OrgClose;
   }
   
   public Double getOrgOpen(){
      return this.OrgOpen;
   }
}
