package vecCluster;

import org.ansj.app.keyword.Keyword;
import org.ansj.recognition.impl.FilterRecognition;
import org.nlpcn.commons.lang.tire.domain.Forest;

import java.io.*;
import java.util.*;

/**
 * Created by codeLearner on 2016/12/19.
 */
public class TextCluster {
    public static void main(String[] args){
        try{
            ComputeFunction cf = new ComputeFunction();
            //加载用户自定义词典
            Forest carDic = new Forest();
            String dicpath = "data/SogouLabDic.dic";
            cf.insertCarDic(carDic, dicpath);
            System.out.println("用户自定义词典加载完毕");
            //加载停用词典
            FilterRecognition fitler = new FilterRecognition();
            String stoppath = "data/stop_word.txt";
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(stoppath)),"utf-8"));
            String line;
            while((line = br.readLine()) != null){
                line = line.trim();
                fitler.insertStopWord(line);
            }
            fitler.insertStopNatures("nr");
            System.out.println("停用词词典加载完毕");
            //读取待聚类的内容
            String textpath = "data/text.txt";
            ArrayList<String> text = new ArrayList<>();
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(textpath)),"gbk"));
            while((line = br.readLine()) != null){
                text.add(line);
            }
            System.out.println("读取待聚类内容完毕");
            //重写了ansj提取关键词的接口
            //提取关键词，并保存所有词
            ArrayList<Keyword[]> textkwds = new ArrayList<>();
            Set<String> allwords = new HashSet<>();
            for(int i =0;i<text.size();i++){
                KeyWordComputer kc = new KeyWordComputer(100);
                List<Keyword> kwdlist = kc.computeTfidfFilter(text.get(i),fitler,0);
                Keyword[] kwsarr = cf.listToArray(kwdlist);
                for(Keyword kw : kwsarr){
                    allwords.add(kw.getName());
                }
                textkwds.add(kwsarr);
            }
            System.out.println("提取关键词完毕");
            //转换成词袋模型
            List<double[]> textmatrix = cf.bagmatrix(textkwds,new ArrayList<>(allwords));
            System.out.println("生成词袋模型完毕");
            //计算文本相似度
            ArrayList<ArrayList<Integer>> clusterid = cf.computeTextSimi(textmatrix);
            System.out.println("文本相似度计算完毕"+clusterid.size());

            //得到最后的聚类结果
            Map<String,ArrayList<String>> result = cf.getClusterResult(text,clusterid,textkwds);

            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File("data/result.txt")),"utf-8");
            for(Map.Entry<String,ArrayList<String>> entry:result.entrySet()){
                writer.write("该类名称:"+entry.getKey()+"    "+"所含文本数量:");
                ArrayList<String> contents = entry.getValue();
                writer.write(contents.size()+"\n");
                for(String str : contents){
                    writer.write(str+"\n");
                }
                writer.flush();
            }
            System.out.println("聚类完毕");

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
