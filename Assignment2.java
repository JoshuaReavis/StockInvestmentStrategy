// Josh Reavis
// Created for CSCI 330 Database Systems
// Fall 2016

// Given an SQL database with Stock Market data, this program runs through the data
// and determines an investment strategy through analysis of the data to provide
// the best days to buy and sell from a given company.

import java.io.*;
import java.util.*;
import java.sql.*;

public class Assignment2{
                                         
   static ArrayList<StockData> StockArray = new ArrayList();
   static boolean foundCompany = false; 
   static Connection conn = null;


   public static void main(String[] args) throws Exception {
      String paramsFile = "ConnectionParameters.txt";                                
      if (args.length >= 1) { 
         paramsFile = args[0];
      } 
      Properties connectprops = new Properties();
      connectprops.load(new FileInputStream(paramsFile));
    
                      
      try { 
         Class.forName("com.mysql.jdbc.Driver");                           
         String dburl = connectprops.getProperty("dburl"); 
         String username = connectprops.getProperty("user"); 
         conn = DriverManager.getConnection(dburl, connectprops);         
         System.out.printf("Database connection %s %s established.%n", dburl, username);
      }catch (SQLException ex) {                                             
            System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n", ex.getMessage(), ex.getSQLState(), ex.getErrorCode());     
      }
   
      Scanner input = new Scanner(System.in);
      String StartDate = null;
      String EndDate = null;
      String ticker = null;
      Double Cash = 0.00;
      int NumberofShares = 0;
      int NumberofTransactions = 0;
     
      while(true){
         System.out.print("Enter a ticker symbol [start/end dates]: ");
         
         //tokenizes the input statement and puts tokens in appropriate variables
         StringTokenizer inputTokens = new StringTokenizer(input.nextLine());
         if (inputTokens.hasMoreTokens()){
            ticker = inputTokens.nextToken();
         }
         if (inputTokens.hasMoreTokens()){
            StartDate = inputTokens.nextToken();
         }
         if (inputTokens.hasMoreTokens()){
            EndDate = inputTokens.nextToken();
         }

         findACompany(ticker);
         if (foundCompany == true){ // continue if the company name was found in the database
            if (StartDate == null && EndDate == null){
                  StockInfoAll(ticker);       
            }else{
            // do same but only the data between start and end dates
               StockInfoDates(ticker, StartDate, EndDate);
            }
            
            // put our ArrayList into ascending order
            Collections.reverse(StockArray);
            
            // populates averages
            for (int i = 0; i < StockArray.size(); i++){
               Double AVG = 0.0;
               if (i >= 50){
                  for (int j = i-50; j < i; j++){
                     AVG += StockArray.get(j).getClose()/50;
                  }
                  StockArray.get(i).setAvg(AVG);
               }
            }
            
            //Transactions 
            for (int i = 50; i < StockArray.size(); i++){
               if (i < StockArray.size()-1){
                  // Buy
                  if ((StockArray.get(i).getClose() < StockArray.get(i).getAvg()) && ((StockArray.get(i).getClose()/StockArray.get(i).getOpen()) < 0.97000001)){
                     NumberofShares += 100;
                     Cash -= (StockArray.get(i+1).getOpen() * 100);
                     NumberofTransactions++;   
                  }
                  // Sell
                  if ((StockArray.get(i).getOpen() > StockArray.get(i).getAvg()) && ((StockArray.get(i).getOpen()/StockArray.get(i-1).getClose()) > 1.00999999) && (NumberofShares >= 100)){
                     NumberofShares -= 100;
                     Cash += (((StockArray.get(i).getOpen() + StockArray.get(i).getClose())/2) * 100);
                     NumberofTransactions++;          
                  }
               }else{ // it's last day - Cash Out
                  Cash += NumberofShares * StockArray.get(i).getOpen();
               }
            }
            // transaction costs
            Cash = Cash - (NumberofTransactions * 8.00);
            
            int splitcount = 0;
            // find all splits and report
            for (int i = StockArray.size()-2; i > -1; i--){
               if (StockArray.get(i).getSplit() != null){
                  System.out.println(StockArray.get(i).getSplit() + " split on " + StockArray.get(i).getDate() + " " + String.format("%.2f", StockArray.get(i).getOrgClose()) + " --> " + String.format("%.2f", StockArray.get(i+1).getOrgOpen()));
                  splitcount++;
               }
            }
            
            System.out.println(splitcount + " splits in " + StockArray.size() + " trading days\n");          
            System.out.println("Executing investment strategy");
            System.out.println("Transactions executed: " + NumberofTransactions);
            System.out.println("Net Cash: " + String.format("%.2f", Cash) + "\n");    
            
            // housecleaning for next iteration   
            StartDate = null;
            EndDate = null;
            NumberofShares = 0;
            Cash = 0.0;
            NumberofTransactions = 0;
            StockArray.clear();
        }                                                    
         
      }
   }
   
   // makes an SQl query to find all the data for a specificed company given by their ticker
   // and populates ArrayList StockArray with all of the relevant data
   private static void StockInfoAll(String ticker) throws SQLException { 
      PreparedStatement pstmt = conn.prepareStatement("select OpenPrice, HighPrice, LowPrice, ClosePrice, TransDate from PriceVolume where Ticker = ? order by TransDate DESC"); //order by Transdate desc
      String SplitFactor;
      Double divisor = 1.0;
      int day = 0;
      
      pstmt.setString(1, ticker);                                                
      ResultSet rs = pstmt.executeQuery();
      
      // for each row in the SQL database given the query
      while(rs.next()) {
         SplitFactor = null;
         Double average = null;
         
         Double currentDivisor = divisor;
         if (!rs.isFirst()){
            
            // grab the opening price of next day and closing of current day
            rs.previous();
            Double NextDayOpeningPrice = rs.getDouble(1);
            rs.next();
            Double ClosingPrice = rs.getDouble(4);
            
            // check for splits
            if (Math.abs(((ClosingPrice/NextDayOpeningPrice) - 2.0)) < 0.20){
               SplitFactor = "2:1";
               divisor *= 2.0;
            }
            if (Math.abs(((ClosingPrice/NextDayOpeningPrice) - 3.0)) < 0.30){
               SplitFactor = "3:1";
               divisor *= 3.0;
            }
            if (Math.abs(((ClosingPrice/NextDayOpeningPrice) - 1.5)) < 0.15){
               SplitFactor = "3:2";
               divisor *= 1.5;
            }
         }
         
       //  System.out.printf(rs.getString(5) + " Open: %.2f, High: %.2f, Low: %.2f, Close: %.2f%n", rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4));
      //   System.out.printf(rs.getString(5) + " Open: %.2f, High: %.2f, Low: %.2f, Close: %.2f%n", rs.getDouble(1)/divisor, rs.getDouble(2)/divisor, rs.getDouble(3)/divisor, rs.getDouble(4)/divisor);

         // add data to our ArrayList
         StockData stockdata = new StockData(SplitFactor, rs.getString(5), rs.getDouble(1)/divisor, rs.getDouble(2)/divisor, rs.getDouble(3)/divisor, rs.getDouble(4)/divisor, average, rs.getDouble(4), rs.getDouble(1)); 
         StockArray.add(stockdata);
      } 

      pstmt.close();                                                             
   }
   
   // makes an SQl query to find the data within a specified range of dates for a specificed company given by their ticker
   // and populates ArrayList StockArray with all of the relevant data
   private static void StockInfoDates(String ticker, String startdate, String enddate) throws SQLException { 
      PreparedStatement pstmt = conn.prepareStatement("select OpenPrice, ClosePrice, HighPrice, LowPrice, TransDate from PriceVolume where Ticker = ? and TransDate BETWEEN ? AND ? order by TransDate DESC");         
      String SplitFactor;
      Double divisor = 1.0;
      int day = 0;
      
      pstmt.setString(1, ticker); 
      pstmt.setString(2, startdate);
      pstmt.setString(3, enddate);                                               
      ResultSet rs = pstmt.executeQuery();
      
      // for each row in the SQL database given the query
      while(rs.next()) {
         SplitFactor = null;
         Double average = null;
         
         Double currentDivisor = divisor;
         if (!rs.isFirst()){
            
            // grab the opening price of next day and closing of current day            
            rs.previous();
            Double NextDayOpeningPrice = rs.getDouble(1);
            rs.next();
            Double ClosingPrice = rs.getDouble(4);
            
            // check for splits
            if (Math.abs(((ClosingPrice/NextDayOpeningPrice) - 2.0)) < 0.20){
               SplitFactor = "2:1";
               divisor *= 2.0;
            }
            if (Math.abs(((ClosingPrice/NextDayOpeningPrice) - 3.0)) < 0.30){
               SplitFactor = "3:1";
               divisor *= 3.0;
            }
            if (Math.abs(((ClosingPrice/NextDayOpeningPrice) - 1.5)) < 0.15){
               SplitFactor = "3:2";
               divisor *= 1.5;
            }
         }
         
       //  System.out.printf(rs.getString(5) + " Open: %.2f, High: %.2f, Low: %.2f, Close: %.2f%n", rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4));
      //   System.out.printf(rs.getString(5) + " Open: %.2f, High: %.2f, Low: %.2f, Close: %.2f%n", rs.getDouble(1)/divisor, rs.getDouble(2)/divisor, rs.getDouble(3)/divisor, rs.getDouble(4)/divisor);

         // add date to our ArrayList
         StockData stockdata = new StockData(SplitFactor, rs.getString(5), rs.getDouble(1)/divisor, rs.getDouble(2)/divisor, rs.getDouble(3)/divisor, rs.getDouble(4)/divisor, average, rs.getDouble(4), rs.getDouble(1)); 
         StockArray.add(stockdata);
      } 

      pstmt.close();                                                                   
   }
   
   // Uses SQL query and confirms if the input ticker was valid
   private static void findACompany(String Ticker) throws SQLException { 
      Statement stmt = conn.createStatement();                                     
      ResultSet results = stmt.executeQuery("select Ticker, Name from Company");
      foundCompany = false;
        
      while (results.next()) {                                                    
         if (results.getString("Ticker").equals(Ticker)){
            System.out.printf("%s%n", results.getString("Name"));
            foundCompany = true;  
         }           
      }
      if (foundCompany == false){
         System.out.println(Ticker + " not found in database.\n");
      }
      stmt.close();
   }  

}
