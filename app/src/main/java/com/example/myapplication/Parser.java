package com.example.myapplication;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.DataVO.ArticleVO;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class Parser extends AsyncTask<Object, List<ArticleVO>, List<ArticleVO>> {
    private String FILE = "jsons/";
    private String title;
    private Context context;
    private final int TODAY = 0;
    private RecycleAdapter adapter;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private final int FIX = 5;

    public Parser(String title, Context context, RecycleAdapter adapter,RecyclerView recyclerView, ProgressBar progressBar) {
        this.title = title;
        this.context = context;
        this.adapter = adapter;
        this.progressBar =progressBar;
        this.recyclerView= recyclerView;
    }

    @Override
    protected void onProgressUpdate(List<ArticleVO>... values) {
        super.onProgressUpdate(values);
        Log.d("jms","progressUpdate");
        adapter.setList(values[0]);
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
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

    private void today_crawling(List<ArticleVO> list, JSONObject body, String base_URL) throws IOException, JSONException {
        String today_class = body.getString("today_class");
        String today_title = body.getString("today_title");
        String today_article_photo = body.getString("today_article_photo");
        String today_article_txt = body.getString("today_article_txt");

        Document document = Jsoup.connect(base_URL).get();
        Elements elements = document.select(today_class);

        for (Element e : elements) {
            Log.d("jms",title);
            String link = e.select("a").attr("href");
            Document temp_document = Jsoup.connect(link).get();
            String img_url = temp_document.select(today_article_photo).attr("src");
            String title = temp_document.select(today_title).text();
            String content = temp_document.select(today_article_txt).html();

            list.add(new ArticleVO(img_url,title,content));

            if(list.size() % FIX == 0){
                publishProgress(list);
            }
        }

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
}
