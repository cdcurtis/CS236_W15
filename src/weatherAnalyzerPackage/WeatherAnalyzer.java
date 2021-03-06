package weatherAnalyzerPackage;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.File;

/**
* http://codingjunkie.net/mapreduce-reduce-joins/
* User: Bill Bejeck
* Date: 6/11/13
* Time: 9:27 PM
* 
*/

/*
 * Project Summary:
 * For this project, we will have two datasets. The first provides station information 
 * for weather stations across the world. The second provides individual recordings for 
 * the stations over a 4-year period. The goal of the project is to find out which 
 * states in the US have the most stable temperature (i.e. their hottest month and 
 * coldest month have the least difference).
 * 
 * Formal Problem:
 * For stations within the United States, group the stations by state. For each state 
 * with readings, find the average temperature recorded for each month (ignoring year)
 * Find the months with the highest and lowest averages for that state. Order the 
 * states by the difference between the highest and lowest month average, ascending.
 * 
 * For each state, return:
 *    The state abbreviation, e.g. “CA”
 *    The average temperature and name of the highest month, e.g. “90, July”
 *    The average temperature and name of the lowest month, e.g. “50, January”
 *    The difference between the two, e.g. “40”
 *    
 * The fields are as follows:
 *    USAF = Air Force station ID. May contain a letter in the first position.
 *    WBAN = NCDC WBAN number
 *    STATION NAME = A text name for the station
 *    CTRY = FIPS country ID
 *    ST = State for US stations
 *    LAT = Latitude in thousandths of decimal degrees
 *    LON = Longitude in thousandths of decimal degrees
 *    ELEV = Elevation in meters
 *    BEGIN = Beginning Period Of Record (YYYYMMDD).
 *    END = Ending Period Of Record (YYYYMMDD).
 *    
 * Readings:
 *    STN---  = The station ID (USAF)
 *    WBAN   = NCDC WBAN number
 *    YEARMODA   = The datestamp
 *    TEMP = The average temperature for the day, followed by the number of recordings
 *    DEWP = Ignore for this project
 *    SLP = Ignore for this project
 *    STP = Ignore for this project
 *    VISIB = Ignore for this project (Visibility)
 *    WDSP = Ignore for this project
 *    MXSPD = Ignore for this project
 *    GUST = Ignore for this project    
 *    MAX = Ignore for this project (Max Temperature for the day)
 *    MIN = Ignore for this project (Min Temperature for the day)
 *    PRCP = Ignore for this project (Precipitation)
 *    NDP = Ignore for this project
 *    FRSHTT = Ignore for this project
 *    
 * Due Date:
 * This project will be due Thursday, March 19th before Midnight. 
 * Email me your submissions, one email per group.
 * 
 * Deliverables:
 *  [ ] A zipped file containing:
 *      [x] The script to run your job
 *      [ ] A README describing your project, including:
 *        [ ] Your usernames and node number.
 *        [ ] An overall description of how you chose to separate the 
 *           problem into different mapreduce jobs, with reasoning.
 *        [ ] A description of each mapreduce job including:
 *           [ ] What the job does 
 *           [ ] An estimate of runtime for the pass
 *        [ ] A description of how you chose to do the join(s)
 *        [ ] A description of anything you did extra for the project, 
 *           such as adding a combiner. If there is anything that you feel 
 *           deserves extra credit, put it here.
 *        [ ] All files needed for the script to run (Don’t include the 
 *           two original datasets).
 *           
 * The script should take as input three things: 
 *  [x] A folder containing the Locations file
 *  [x] A folder containing the Recordings files
 *  [x] An output folder
 *  
 *  I will provide these when I try to run your script. Please change 
 *  the hdfs interactions to be based on /user/sjaco002 before you submit 
 *  (it should be your own folder when you run it).
 *  
 * Think About:
 *  [x] There are different ways to do joins in mapreduce. What can you use 
 *      from the datasets to inform your decision (e.g. size)? Please Google 
 *      joins in mapreduce for more information
 *  [x] Make sure your parse correctly. One file is a csv, the other is not. 
 *      One file has a single row of Headers, the other has many.
 *  [x] How many passes should you do? How much work should each pass do?
 *  [x] Make sure to start early. The cluster will get slower as more people 
 *      use it. A single pass of the data took me about 2 minutes while the 
 *      cluster was empty.
 *    
 * Potential For Extra Credit:
 * Please feel free to try to beef up your project for extra credit. There 
 * are many ways that you can do this. Here are a few examples:
 *  [ ] A good use of combiners
 *  [ ] A clever way to achieve faster execution time
 *  [x] Enriching the data, e.g. including the average precipitation for the two months
 * 
 * Bigger Bonus:
 * [ ] Include the stations with “CTRY” as “US” that don’t have a state tag, 
 *    finding a way to estimate the state using a spatial distance with the 
 *    known stations. There are some stations that are Ocean Buoys so you may 
 *    want to have a maximum distance to be required in order to be included 
 *    in a state, or you could create a separate “state” representing the 
 *    “pacific” and “atlantic” ocean (Checked by using coordinates). 
 *    There is a lot of potential work here so the extra credit could be large).
 * 
 * Whatever you try to do let me know in your README. If you aren’t sure 
 * whether your idea is worth extra credit or not, just email me.
 *    
 * I've had a few people ask me questions about what the "correct" answer for this project is. 
 * There are a few pieces of variance on this project. For example:
 * 
 *    1. We have one station ID that corresponds to several stations. 
 *      Some groups are handling this in different ways
 *    2. Some students have different ideas of how to "correctly" calculate average. 
 *      This is okay. The important thing is that your results need to be correct 
 *      given the choices that you have made. For a general idea of what results 
 *      should look like, states like Utah, the Dakotas, and Minnesota should be near 
 *      the bottom of the least (High variance in temperature) whereas states like 
 *      Hawaii, California, and Puerto Rico should be near the top of the list (Stable 
 *      year-round temperatures).
 * 
 * A note on the values for precipitation (For EXTRA CREDIT ONLY):
 * A value of 99.99 means that there is no data (You can either treat these as 0 or 
 * exclude them from your calculations, though the latter is the more correct option).
 * 
 * There is a letter at the end of the recordings. Here is a table of what the letters mean 
 * (Basically the letter tells you how long the precipitation was accumulated before recording).
 * 
 *    A - 6 hours worth of precipitation
 *    B - 12 hours...
 *    C - 18 hours...
 *    D - 24 hours...
 *    E - 12 hours... (slightly different from B but the same for this project).
 *    F - 24 hours ... (slightly different from D but the same for this project).
 *    G - 24 hours ... (slightly different from D but the same for this project).
 *    H - station recorded a 0 for the day (although there was some recorded instance of precipitation).
 *    I - station recorded a 0 for the day (and there was NO recorded instance of precipitation).
 * 
 * How you treat these is up to you (just let me know in your README). A simple solution 
 * would be to multiply. For instance, if they recorded 12 hours worth of precipitation, 
 * multiply it by 2 to extrapolate 24 hours worth.
 * 
 * I didn't specifically say this, so please include in your report the output file 
 * from your run of your project. Should be a single file with 50-53 lines (Depending 
 * on if you count things like Puerto Rico as "states") with a schema similar to the 
 * following (You can add a header row if you would like):
 * 
 *    STATE - abbreviation of the state, "CA"
 *    HIGH MONTH NAME - month with the highest average, "JULY"
 *    AVERAGE FOR THAT MONTH - "73.668"
 *    **PRECIPITATION THAT MONTH
 *    LOW MONTH NAME - month with the lowest average, "DECEMBER"
 *    AVERAGE FOR THAT MONTH - "48.054"
 *    **PRECIPITATION THAT MONTH
 *    DIFFERENCE BETWEEN THE TWO - "25.614"
 *    
 *    **example of extra credit fields
 *    
 *    What's left:
[x] Sort output
[x] Add timers for each map/reduce phase
[ ] Translate state ID to name and month number to month (easy, just haven't gotten to it yet.
[ ] confirm the calculation of the temp ranges (right now, Puerto Rico shows a variance of ~1 degree?!!)
[ ] batch script

Extra credit:
[ ] define lat/lon boundaries for the US + territories and filter and assign locations to stations that are not listed as US
[ ] look at Combiners Partitioners to see if possible to accelerate execution 
    (will use the base version to time and compare against
[X] I am tracking precipitation already (i was already running similar code, just added it in)
[ ] I think it would be fairly easy to track the min/max  temp, rainfall for /yearmonthdate 
    with location to show the extremes for the state versus the average
    
    hdfs dfs -rm -r output
    hdfs dfs -rm -r output_join
    hdfs dfs -rm -r output_state_data
    hdfs dfs -rm -r output_us_data
    
 */
public class WeatherAnalyzer {
  
  public static int deleteDirectory(String directoryName) {
    File index = new File(directoryName);
    if (index.exists()) {
      String[]entries = index.list();
      for(String s: entries){
        File currentFile = new File(index.getPath(),s);
        currentFile.delete();
      }
      index.delete();
      return 0;
    } 
  return -1;
  }
  
  public static void main(String[] args) throws Exception {
    
    Configuration config = new Configuration();
       
    // Vars to hold parameter info
    String readingsDir,stationsDir, joinDir, mapUSDir, mapStateDir, outputDir;
    readingsDir = stationsDir = "";
    joinDir = "output_join";
    mapUSDir = "output_us_data";
    mapStateDir = "output_state_data";
    outputDir = "output"; // Set as Default
    final Double NANO2SEC = (double) 1000000000; // for converting nanoseconds to seconds
    Double duration = (Double) 0.0;
    long startTime = System.nanoTime();
    long endTime = System.nanoTime();
    long jobStartTime = System.nanoTime();
    long jobEndTime = System.nanoTime();
    Double jobDuration = (Double) 0.0;    
    
    // Retrieve and parse passed in parameters
    for (int i = 0; i < args.length; i++) {

      if (args[i].substring(0, 1).equals("-")) {
        String catchMe = args[i].substring(1, 2);
        
        if (catchMe.equals("r")) {
          readingsDir = args[i + 1];
        } else if (catchMe.equals("s")) {
          stationsDir = args[i + 1];
        } else if (catchMe.equals("o")) {
          outputDir = args[i + 1];
        } else {
          // Nothing
        }
      }
    }
    
    /*
     *  clear previous passes if present
     
    deleteDirectory(joinDir);
    deleteDirectory(mapUSDir);
    deleteDirectory(mapStateDir);
    deleteDirectory(outputDir);
    */
    
    /*
     * Define ReduceJoin job
     * Maps stations and readings, filtering data as necessary and then joining
     */
    
    // Begin timer
    jobStartTime = System.nanoTime();
    String filePaths = stationsDir + "/," + readingsDir + "/";
    
    Job joinDataSets = new Job(config, "Join");
    joinDataSets.setJarByClass(WeatherAnalyzer.class);

    //FileInputFormat.addInputPaths(joinDataSets, filePaths.toString());
    FileInputFormat.addInputPaths(joinDataSets, filePaths);
    FileOutputFormat.setOutputPath(joinDataSets, new Path(joinDir));

    joinDataSets.setMapperClass(MapperForJoin.class);
    joinDataSets.setReducerClass(ReducerForJoin.class);
    joinDataSets.setPartitionerClass(AnchorPartitioner.class);
    joinDataSets.setGroupingComparatorClass(AnchorGroupComparator.class); // remove for many to many
    joinDataSets.setOutputKeyClass(AnchorKey.class);
    joinDataSets.setOutputValueClass(Text.class);
    if ( joinDataSets.waitForCompletion(true) ) {
      
      jobEndTime = System.nanoTime();   
      jobDuration = (double) ((jobEndTime - jobStartTime) / NANO2SEC);  
      System.out.print("Join completed in " + String.format("%.4f", jobDuration) + "secs.\n");
    
    } else {
      System.err.print("Something went horribly wrong...\n");
    }
       
    /*
     * Map results from previous run to consolidate by state, year/month, 
     * eliminating non-US results at this time
     * reduce to max/min values per month
     *    
     */ 
   
    jobStartTime = System.nanoTime();
    
    Configuration getStateDataConf = new Configuration();
    Job getStateData = new Job(getStateDataConf, "Get States' data");
    getStateData.setJarByClass(WeatherAnalyzer.class);

    getStateData.setMapperClass(MapperUSdata.class);
    getStateData.setReducerClass(ReducerUSdata.class);

    getStateData.setOutputKeyClass(Text.class);
    getStateData.setOutputValueClass(Text.class);

    getStateData.setInputFormatClass(TextInputFormat.class);
    getStateData.setOutputFormatClass(TextOutputFormat.class);

    TextInputFormat.addInputPath(getStateData, new Path(joinDir));
    TextOutputFormat.setOutputPath(getStateData, new Path(mapUSDir));

    // Run Job
    if ( getStateData.waitForCompletion(true) ) {

      jobEndTime = System.nanoTime();   
      jobDuration = (double) ((jobEndTime - jobStartTime) / NANO2SEC);  
      System.out.print("getStatesData completed in " + String.format("%.4f", jobDuration) + "secs.\n");
    
    } else {
      System.err.println("Something went horribly wrong...");
    }

    /*
     * Map results from previous run to consolidate by state, year/month, 
     * eliminating non-US results at this time
     * reduce to max/min values per month
    */
    
    jobStartTime = System.nanoTime();
    
    Configuration compileDataConf = new Configuration();
    Job compileData = new Job(compileDataConf, "Get States data");
    compileData.setJarByClass(WeatherAnalyzer.class);

    compileData.setMapperClass(MapStateData.class);
    compileData.setReducerClass(ReduceStateData.class);

    compileData.setOutputKeyClass(Text.class);
    compileData.setOutputValueClass(Text.class);

    compileData.setInputFormatClass(TextInputFormat.class);
    compileData.setOutputFormatClass(TextOutputFormat.class);
    //compileData.setSortComparatorClass(Text.class);

    TextInputFormat.addInputPath(compileData, new Path(mapUSDir));
    TextOutputFormat.setOutputPath(compileData, new Path(mapStateDir));

    // Run Job
    if ( compileData.waitForCompletion(true) ) {
      
      jobEndTime = System.nanoTime();   
      jobDuration = (double) ((jobEndTime - jobStartTime) / NANO2SEC);  
      System.out.print("compileData completed in " + String.format("%.4f", jobDuration) + "secs.\n");
      
    } else {
      System.err.println("Something went horribly wrong...");
    }
      
    
    /*
     * Take output State data, map on temp difference and output results
    */
    jobStartTime = System.nanoTime();
    
    Configuration outputResulstsConf = new Configuration();
    Job outputResults = new Job(outputResulstsConf, "Output results");
    outputResults.setJarByClass(WeatherAnalyzer.class);

    outputResults.setMapperClass(MapperOutResults.class);
    outputResults.setReducerClass(ReducerOutResults.class);

    outputResults.setOutputKeyClass(Text.class);
    outputResults.setOutputValueClass(Text.class);

    outputResults.setInputFormatClass(TextInputFormat.class);
    outputResults.setOutputFormatClass(TextOutputFormat.class);

    TextInputFormat.addInputPath(outputResults, new Path(mapStateDir));
    TextOutputFormat.setOutputPath(outputResults, new Path(outputDir));

    // Run Job
    if ( outputResults.waitForCompletion(true) ) {
      
      jobEndTime = System.nanoTime();   
      jobDuration = (double) ((jobEndTime - jobStartTime) / NANO2SEC);  
      System.out.print("outputResulstsConf completed in " + String.format("%.4f", jobDuration) + "secs.\n");
      
      endTime = System.nanoTime();   
      duration = (double) ((endTime - startTime)/ NANO2SEC);
      System.out.println();
      System.out.println("Analysis complete in " + String.format("%.4f", duration) + "secs.");
      
    } else {
      System.err.println("Something went horribly wrong...");
    }
    
  }
  
}