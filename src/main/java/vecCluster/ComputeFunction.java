package vecCluster;

import org.ansj.app.keyword.Keyword;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.nlpcn.commons.lang.tire.domain.Forest;
import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;

import java.io.*;
import java.util.*;

/**
 * Created by  codeLearner on 2016/12/19.
 */
public class ComputeFunction {
    /**
     * 加载用户自定义词典
     * @param carDic
     * @param carDicRealPath
     * @throws java.io.IOException
     */
    public static void insertCarDic(Forest carDic, String carDicRealPath) throws IOException {
        BufferedReader ar = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(carDicRealPath)), "utf-8"));
        String Line = null;
        while ((Line = ar.readLine()) != null) {
            String[] seg = Line.split("\t");

            if (seg.length == 3) {
                Library.insertWord(carDic, new Value(seg[0], seg[1], seg[2]));
            } else if (seg.length == 1) {
                if (seg[0].length() > 3)
                    continue;
                Library.insertWord(carDic, new Value(Line.toLowerCase(), "userdefine", "2000"));
            }
        }
        ar.close();
        System.out.println(ToAnalysis.parse("download over"));
    }
    /**
     * 将list转换成数组
     * @param list
     * @return
     */
    public Keyword[] listToArray(List<Keyword> list){
        Keyword[] wordarray = new Keyword[list.size()];
        for(int i = 0;i<list.size();i++){
            wordarray[i] = list.get(i);
        }
        return wordarray;
    }

    /**
     * 转换成词袋模型
     * @param textkwds
     * @param allwords
     * @return
     */
    public List<double[]> bagmatrix(List<Keyword[]> textkwds,List<String> allwords){
        List<double[]> textmatrix = new ArrayList<>();
        for(int i =0;i<textkwds.size();i++){
            double[] vec = new double[allwords.size()];
            Keyword[] text = textkwds.get(i);
            for(Keyword kw : text){
                if(allwords.contains(kw.getName())){
                    int j = allwords.indexOf(kw.getName());
                    vec[j] = 1;
                }
            }
            textmatrix.add(vec);
        }
        return textmatrix;
    }
    /**
     * 计算向量长度
     * @param vec
     * @return
     */
    public double getVetLen(double[] vec){
        double len = 0;
        for(int i=0;i<vec.length;i++){
            len += vec[i]*vec[i];
        }
        len = Math.sqrt(len);
        return len;
    }

    /**
     * 计算相似度
     * @param vec1
     * @param vec2
     * @return
     */
    public double getSimilar(double[] vec1,double[] vec2){
        double simi = 0;
        if(vec1.length!=vec2.length){
            System.out.println("the matric are not cossitant");

        }else{
            for(int i =0;i<vec1.length;i++){
                simi += vec1[i]*vec2[i];
            }
            simi = simi/(getVetLen(vec1)*getVetLen(vec2));

        }
        return simi;
    }

    /**
     * 更新中心点
     * @param points
     * @return
     */
    public double[] updateCenter(ArrayList<double[]> points){
        double[] newcenter = new double[points.get(0).length];
        for(int i =0;i<points.size();i++){
            double[] point = points.get(i);
            for(int j = 0;j<point.length;j++){
                newcenter[j] += point[j];
            }
        }
        for(int i =0;i<newcenter.length;i++){
            newcenter[i] = newcenter[i]/points.size();
        }
        return newcenter;
    }
    /**
     * 根据id判断是否已经聚类过
     * @param clusterids
     * @param id
     * @return
     */
    public boolean isClustered(ArrayList<ArrayList<Integer>> clusterids,Integer id){
        boolean ischecked = false;
        for(int i =0;i<clusterids.size();i++){
            ArrayList<Integer> ids = clusterids.get(i);
            if(ids.contains(id)){
                ischecked = true;
                break;
            }
        }
        return ischecked;
    }

    /**
     * 计算文档相似度
     * @param textmatrix 文档矩阵
     * @return
     */
    public ArrayList<ArrayList<Integer>> computeTextSimi(List<double[]> textmatrix){
        ArrayList<ArrayList<Integer>> clusterid = new ArrayList<>();
        for(int i =0;i<textmatrix.size();i++){
            if(!isClustered(clusterid, i)){
                ArrayList<double[]> index = new ArrayList<>();
                ArrayList<Integer> idinex = new ArrayList<>();
                index.add(textmatrix.get(i));
                idinex.add(i);
                for (int j = i + 1; j < textmatrix.size(); j++) {
                    if (!isClustered(clusterid, j)) {
                        double simi = getSimilar(textmatrix.get(i), textmatrix.get(j));
                        if (simi > 0.5) {
                            index.add(textmatrix.get(j));
                            idinex.add(j);
                            textmatrix.set(i, updateCenter(index));
                        }

                    }
                }
                if (idinex.size() > 1) {
                    clusterid.add(idinex);
                }

            }

        }
        return clusterid;
    }

    /**
     * 给每个类确定类名以及对应的文本内容
     * @param text
     * @param clusterid
     * @param textkwds
     * @return
     */
    public Map<String,ArrayList<String>> getClusterResult(ArrayList<String> text,ArrayList<ArrayList<Integer>> clusterid,
                                                          ArrayList<Keyword[]> textkwds){
        Map<String,ArrayList<String>> result = new HashMap<>();
        for(int i =0;i<clusterid.size();i++){
            ArrayList<Integer> temp = clusterid.get(i);
            ArrayList<String> onecluster = new ArrayList<>();
            Map<String,Double> wordscore = new HashMap<>();
            String clustername="";
            for(Integer no:temp){
                onecluster.add(text.get(no));
                Keyword[] sent = textkwds.get(no);
                for(int j = 0;j<sent.length;j++){
                    if(!wordscore.keySet().contains(sent[j].getName())){
                        wordscore.put(sent[j].getName(),sent[j].getScore());
                    }else{
                        double score = wordscore.get(sent[j].getName());
                        if(score<sent[j].getScore()){
                            wordscore.put(sent[j].getName(),sent[j].getScore());
                        }
                    }
                }
            }

            //将关键词按照得分从高到低排序
            List<Map.Entry<String,Double>> list = new ArrayList<>(wordscore.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                //降序排序
                public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });
            //取每一类前6个关键词作为标题
            int k =0;
            for(Map.Entry<String,Double> mapping:list){
                if(k<6){
                    clustername += mapping.getKey()+" ";
                }
                k++;
            }
            result.put(clustername,onecluster);
            System.out.println("第"+i+"类的标题---"+clustername);
        }
        return result;
    }

}
