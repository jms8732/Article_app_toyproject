package com.example.myapplication;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DataVO.ArticleVO;
import com.example.myapplication.Parser.ParserHelper;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ArticleCrawler extends AsyncTask<Object, List<ArticleVO>, List<ArticleVO>> {
    private String FILE = "jsons/";
    private String title;
    private Context context;
    private final int TODAY = 0;
    private RecycleAdapter adapter;
    private RecyclerView recyclerView;
    private  ProgressBar progressBar;

    public ArticleCrawler(String title, Context context, RecycleAdapter adapter, RecyclerView recyclerView, ProgressBar progressBar) {
        this.title = title;
        this.context = context;
        this.adapter = adapter;
        this.recyclerView = recyclerView;
        this.progressBar = progressBar;
    }


    @Override
    protected void onProgressUpdate(List<ArticleVO>... values) {
        super.onProgressUpdate(values);

        progressBar.setVisibility(View.GONE);
        adapter.setList(values[0]);
        adapter.notifyDataSetChanged();
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected List<ArticleVO> doInBackground(Object... objects) {
        int state = (int) objects[0];
        List<ArticleVO> list = (List<ArticleVO>) objects[1];
        JSONObject object = read_JSON_File();
        String base_URL = null;

        try {
            base_URL = object.getString("index");
            choose_state(object, state, list, base_URL);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    protected void onPostExecute(List<ArticleVO> articleVOList) {
        adapter.setList(articleVOList);
        adapter.notifyDataSetChanged();
    }

    //고른 아이콘에 맞는 크롤링
    private void choose_state(JSONObject head, int state, List<ArticleVO> list, String base_URL) {
        JSONObject body = null;
        try {
            if (state == TODAY) {
                body = head.getJSONObject("today");
                today_crawling(list, body, base_URL);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //오늘의 기사 크롤링
    private void today_crawling(List<ArticleVO> list, JSONObject body, String base_URL) throws IOException, JSONException {
        String today_class = body.getString("today_class");
        String today_title = body.getString("today_title");
        String today_article_photo = body.getString("today_article_photo");
        String today_article_txt = body.getString("today_article_txt");

        //TEST용 데이터
        List<String> temp = new ArrayList<>();

        /*//Ytn
        temp.add("https://www.ytn.co.kr/_ln/0101_202011031707277328");
        temp.add("https://www.ytn.co.kr/_ln/0104_202011041709178451");*/


     /*   for (String src : temp) {
            Document doc = Jsoup.connect(src).get();
            String title = doc.select(today_title).text();
            String img_url = doc.select(today_article_photo).attr("src");
            String content = doc.select(today_article_txt).html();

            list.add(new ArticleVO(this.title, img_url, title, content));
            publishProgress(list);
        }*/

          //실제 데이터
        Elements elements = Jsoup.connect(base_URL).get().select(today_class);

        for (Element e : elements) {

            //todo 크롤링 시, 오류 처리
            try {
                String link = e.select("a").attr("href");
                Document temp_document = makeDocument(base_URL, link);

                if(temp_document != null) {
                    String img_url = makeImageURL(base_URL, temp_document.select(today_article_photo).attr("src"));
                    String title = temp_document.select(today_title).html();
                    String content = temp_document.select(today_article_txt).html();
                    content = replaceAll(content);

                    list.add(new ArticleVO(this.title, img_url, title, content));

                        publishProgress(list);

                }
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
    }

    private String makeImageURL(String baseURL, String url) {
        String ret = null;
        if (title.equals("ytn")) {
            if (!url.contains("image.ytn.co.kr"))
                ret = baseURL + url;
            else
                ret = url;
        } else
            ret = url;

        return ret;
    }

    private Document makeDocument(String baseUrl, String link) throws IOException {
        Document ret = null;
        if (title.equals("ytn")) {
            ret = Jsoup.connect(baseUrl + link).get();
        } else if(!link.isEmpty())
            ret = Jsoup.connect(link).get();

        return ret;
    }

    //JSON 파일을 읽어 들인다
    private JSONObject read_JSON_File() {
        AssetManager manager = context.getAssets();
        JSONObject ret = null;
        try {
            FILE += title + ".json";
            InputStream is = manager.open(FILE);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isr);

            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            ret = new JSONObject(sb.toString()).getJSONObject(title);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private String replaceAll(String text) {
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&quot;", "\"");

        return text;
    }

}
