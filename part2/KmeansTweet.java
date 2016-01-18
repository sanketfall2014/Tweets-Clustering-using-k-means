/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Sanket
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
 class TweetAttributes {

    private String text;
    private long id;
    private String from_user;
    private String created_at;
    private int clusterId;
    private long centroidId;

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFrom_user() {
        return from_user;
    }

    public void setFrom_user(String from_user) {
        this.from_user = from_user;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public long getCentroidId() {
        return centroidId;
    }

    public void setCentroidId(long centroidId) {
        this.centroidId = centroidId;
    }


}
class KMeansJaccard {

    private int k;

    private List<TweetAttributes> tweetList;
    private List<TweetAttributes> centroidList;
    private Map<Long, TweetAttributes> tweetMap;

    public List<TweetAttributes> getCentroidList() {
        return centroidList;
    }

    public void setCentroidList(List<TweetAttributes> centroidList) {
        this.centroidList = centroidList;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public List<TweetAttributes> getTweetList() {
        return tweetList;
    }

    public void setTweetList(List<TweetAttributes> tweetList) {
        this.tweetList = tweetList;
    }

    public Map<Long, TweetAttributes> getTweetMap() {
        return tweetMap;
    }

    public void setTweetMap(Map<Long, TweetAttributes> tweetMap) {
        this.tweetMap = tweetMap;
    }

    public KMeansJaccard(int k) {
        this.k = k;
        tweetList = new ArrayList<>();
        tweetMap = new HashMap<>();
        centroidList = new ArrayList<>();
    }

    public void makeTweetObjects(String inputFile) throws IOException, ParseException,JSONException  {
        File file = new File(inputFile);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;

        while ((str = br.readLine()) != null) {
            JSONObject jsonObject = new JSONObject(str);
            TweetAttributes tweetAttributes = new TweetAttributes();
            tweetAttributes.setCreated_at(jsonObject.getString("created_at"));
            tweetAttributes.setFrom_user(jsonObject.getString("from_user"));
            tweetAttributes.setText(jsonObject.getString("text"));
            long id = jsonObject.getLong("id");
            tweetAttributes.setId(id);

            tweetMap.put(id, tweetAttributes);
            tweetList.add(tweetAttributes);
        }
        br.close();
    }

    public List<TweetAttributes> getInitialCentroids(String initailCentroidsFile) throws IOException {
        File file = new File(initailCentroidsFile);

        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;

        while ((str = br.readLine()) != null) {
            if (str.endsWith(",")) {
                str = str.substring(0, str.length() - 1);
            }
            TweetAttributes tweetAttributes = tweetMap.get(Long.valueOf(str));
            centroidList.add(tweetAttributes);
        }
        br.close();
        return centroidList;
    }

    public double getJaccardDistance(final TweetAttributes tweet1, final TweetAttributes tweet2) {
        double jaccardDistance = 0.0;
        String[] tweetText1 = tweet1.getText().split("\\s");
        String[] tweetText2 = tweet2.getText().split("\\s");

        Set<String> ts1 = new HashSet<>();
        Set<String> ts2 = new HashSet<>();

        for (String wordInTweet1 : tweetText1) {
            ts1.add(wordInTweet1);
        }

        for (String wordInTweet2 : tweetText2) {
            ts2.add(wordInTweet2);
        }

        List<String> t1 = new ArrayList<>();
        t1.addAll(ts1);
        List<String> t2 = new ArrayList<>();
        t2.addAll(ts2);

        Collections.sort(t1);
        int intersect = 0;
        for (String s : t2) {
            if (Collections.binarySearch(t1, s) >= 0) {
                intersect++;
            }
        }
        int totalSize = t1.size() + t2.size();
        int union = totalSize - intersect;
        jaccardDistance = 1 - ((double) intersect / union);
        return jaccardDistance;
    }

    public void assignClusterToTweet(Long clusterId, List<Double> list) {
        double min = Integer.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            if (min > list.get(i)) {
                index = i;
                min = list.get(i);
            }
        }
        tweetMap.get(clusterId).setClusterId(index + 1);
    }
}

public class KmeansTweet {

    public static void main(String[] args) throws IOException, ParseException, JSONException {

        int k = Integer.parseInt(args[0]);
        String inputFile = args[1];
        String initialSeedsFile = args[2];
        String outputFile = args[3];

        KMeansJaccard kmj = new KMeansJaccard(k);
        kmj.makeTweetObjects(inputFile);
        List<TweetAttributes> centroidPoints = kmj.getInitialCentroids(initialSeedsFile);

        Map<Long, List<Double>> distanceMap = new LinkedHashMap<>();
        List<TweetAttributes> tweets = kmj.getTweetList();

        buildDistanceMap(centroidPoints, tweets, kmj, distanceMap);

        for (Long i : distanceMap.keySet()) {
            kmj.assignClusterToTweet(i, distanceMap.get(i));
        }

        List<TweetAttributes> tweetCopy = tweets;
        List<TweetAttributes> centroidCopy = centroidPoints;

        double cost = calculateCost(tweets, centroidPoints, kmj);

        Map<Integer, List<TweetAttributes>> tweetClusterMapping = getTweetCLusterMapping(tweets);

        for (Integer i : tweetClusterMapping.keySet()) {
            List<TweetAttributes> ta = tweetClusterMapping.get(i);
            for (TweetAttributes tweet : ta) {
                centroidPoints.remove(i - 1);
                centroidPoints.add(i - 1, tweet);

                distanceMap.clear();
                buildDistanceMap(centroidPoints, tweets, kmj, distanceMap);

                for (Long id : distanceMap.keySet()) {
                    kmj.assignClusterToTweet(id, distanceMap.get(id));
                }

                double newCost = calculateCost(tweets, centroidPoints, kmj);

                if (newCost < cost) {
                    cost = newCost;
                    tweetCopy = kmj.getTweetList();
                    centroidCopy = kmj.getCentroidList();
                }

            }
        }
        double sse = calculateSSE(tweetCopy, centroidCopy, kmj);
        System.out.println("COST: " + cost + " " +"SSE:"+sse);
        writeOutputToFile(tweetCopy, outputFile);
        
                
        
        
        
        
        
        
        
    }

    private static double calculateSSE(List<TweetAttributes> tweets, List<TweetAttributes> centroidPoints, KMeansJaccard kmj) {
double sse = 0.0;

        Map<Integer, List<TweetAttributes>> clusterMap = getTweetCLusterMapping(tweets);




        for (Integer clusterId : clusterMap.keySet()) {

            TweetAttributes centroid = centroidPoints.get(clusterId - 1);

            List<TweetAttributes> tweetsInCluster = clusterMap.get(clusterId);




            for (TweetAttributes tweet : tweetsInCluster) {

                double jaccardDistance = kmj.getJaccardDistance(centroid, tweet);

                sse += jaccardDistance * jaccardDistance;

            }

        }

        return sse;
    }

    private static Map<Integer, List<TweetAttributes>> getTweetCLusterMapping(List<TweetAttributes> tweets) {
        Map<Integer, List<TweetAttributes>> clusterMap = new LinkedHashMap<Integer, List<TweetAttributes>>();

        for (TweetAttributes te : tweets) {
            if (clusterMap.containsKey(te.getClusterId())) {
                List<TweetAttributes> clusterPoints = clusterMap.get(te.getClusterId());
                clusterPoints.add(te);
                clusterMap.put(te.getClusterId(), clusterPoints);
            } else {
                List<TweetAttributes> newClusterPoint = new ArrayList<>();
                newClusterPoint.add(te);
                clusterMap.put(te.getClusterId(), newClusterPoint);
            }
        }
        return clusterMap;
    }

    private static double calculateCost(List<TweetAttributes> tweets, List<TweetAttributes> centroidPoints, KMeansJaccard kmj) {
        double distance = 0.0;
        Map<Integer, List<TweetAttributes>> clusterMap = getTweetCLusterMapping(tweets);

        for (Integer clusterId : clusterMap.keySet()) {
            TweetAttributes centroid = centroidPoints.get(clusterId - 1);
            List<TweetAttributes> tweetsInCluster = clusterMap.get(clusterId);

            for (TweetAttributes tweet : tweetsInCluster) {
                distance += kmj.getJaccardDistance(centroid, tweet);
            }
        }
        return distance;
    }

    private static void writeOutputToFile(List<TweetAttributes> tweets, String outputFile) throws IOException {
               		try {

			File file = new File(outputFile); //Your file

			FileOutputStream fos = new FileOutputStream(file);

			PrintStream ps = new PrintStream(fos);

			System.setOut(ps);

		} catch (FileNotFoundException e) {

			e.printStackTrace();

		}
       
         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile))); 
    //    List<Long> output=new ArrayList<>();
      //  int count=1;
        Map<Integer,ArrayList> opmap = new HashMap<Integer,ArrayList>();
        
        
        for(TweetAttributes user : tweets) {        
    if(opmap.containsKey(user.getClusterId())) {
        opmap.get(user.getClusterId()).add(user.getId());
    } else {
        ArrayList<Long> ids = new ArrayList<Long>();
        ids.add(user.getId());
        opmap.put(user.getClusterId(), ids);
    }
  //  count++;
}

     
     /*   for(TweetAttributes model : tweets) {
            
           System.out.println(model.getClusterId()+" "+model.getId());
            
        }*/
     //   System.out.println(outputFile+"  "+out);
       for (Entry<Integer, ArrayList> entry : opmap.entrySet()) {
        System.out.print(entry.getKey()+"   ");
        for(Object fruitNo : entry.getValue()){
        System.out.print(fruitNo);
        System.out.print(", ");
        }
        System.out.println();
    }

      
}
        
         
        
        

    

    private static void buildDistanceMap(List<TweetAttributes> centroidPoints, List<TweetAttributes> tweets, KMeansJaccard kmj,
                                         Map<Long, List<Double>> distanceMap) {

        for (TweetAttributes t1 : centroidPoints) {
            for (TweetAttributes t2 : tweets) {
                double jaccardDistance = kmj.getJaccardDistance(t1, t2);
                if (distanceMap.containsKey(t2.getId())) {
                    List<Double> disFromCentroids = distanceMap.get(t2.getId());
                    disFromCentroids.add(jaccardDistance);
                } else {
                    List<Double> list = new ArrayList<>();
                    list.add(jaccardDistance);
                    distanceMap.put(t2.getId(), list);
                }
            }
        }
    }

}

