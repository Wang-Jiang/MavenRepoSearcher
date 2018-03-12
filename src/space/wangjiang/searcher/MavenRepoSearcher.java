package space.wangjiang.searcher;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MavenRepoSearcher extends AnAction {

    private Project project = null;

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        String name = askForName();
        if (name == null) {
            return;
        }
        showRepoListWindow(searchRepo(name));
    }

    private String askForName() {
        return Messages.showInputDialog(project, "输入项目名", "输入项目名", Messages.getQuestionIcon());
    }
    private List<Repo> searchRepo(String repoName) {
        List<Repo> list = new ArrayList<>();
        //如果是空格隔开的，如common io，搜索q=common+io
        String searchUrl = "http://mvnrepository.com/search?q=";
        repoName = repoName.replace(' ', '+');
        try {
            Document document = Jsoup.connect(searchUrl + repoName).get();
            //        System.out.println(document.body());
            Elements elements = document.body().select("div[class=im]");
            for (Element element : elements) {
                Repo repo = new Repo();
                Element title = element.selectFirst("h2[class=im-title]");
                if (title == null) continue;
                Element name = title.selectFirst("a");
                print(name.text());
                repo.setName(name.text());

                Element link = element.selectFirst("a[href]");
                if (link == null) continue;
                String url = link.attr("abs:href");
                print(url);
                repo.setUrl(url);
                list.add(repo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void showRepoListWindow(List<Repo> dataList) {
        showListFrame("搜索结果", dataList, (frame, index) -> {
            showVersionListWindow(dataList.get(index));
            frame.dispose();
        });
    }

    private void showVersionListWindow(Repo repo) {
        List<Version> list = getRepoVersionList(repo.getUrl());
        showListFrame("版本列表:" + repo.getName(), list, (frame, index) -> {
            showMavenConfig(list.get(index).getUrl());
            frame.dispose();
        });
    }

    /**
     * 获取Repo的版本列表
     */
    private List<Version> getRepoVersionList(String url) {
        List<Version> versionList = new ArrayList<>();
        try {
            Document document = Jsoup.connect(url).get();
            Element versions = document.selectFirst("div[class=gridcontainer]");
            Elements trs = versions.select("tr");
            for (Element tr : trs) {
                Element td = tr.selectFirst("a[href]");
                if (td != null) {
                    Version version = new Version(td.text(), td.attr("abs:href"));
                    versionList.add(version);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return versionList;
    }

    private void showMavenConfig(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Element textarea = document.selectFirst("textarea[id=maven-a]");
            String mavenConfig = textarea.text();
            JTextArea textArea = new JTextArea();
            textArea.setText(mavenConfig);
            textArea.setFont(new Font(null, Font.PLAIN, 16));
            print(mavenConfig);
            JFrame frame = createFrame("Maven配置");
            frame.setContentPane(textArea);
            frame.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private <T> void showListFrame(String title, List<T> dataList, OnListSelectListener listener) {
        JFrame frame = createFrame(title);
        JList<T> list = new JBList<>(dataList);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);//设置单选
        list.setSelectionBackground(JBColor.LIGHT_GRAY);//选中颜色
        list.setFixedCellHeight(30);
        list.setFont(new Font(null, Font.PLAIN, 16));
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                //鼠标按下和抬起都算做事件
                int index = list.getSelectedIndex();
                print("选中" + index + "  " + dataList.get(index));
                listener.onSelect(frame, index);
            }
        });

        JScrollPane scrollPane = new JBScrollPane(list);
        frame.setContentPane(scrollPane);
        frame.setVisible(true);//这个需要放在最后调用否则，里面的内容不显示
    }

    private JFrame createFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        return frame;
    }

    private void print(Object o) {
//        System.out.println(o);
    }

    public static void main(String[] args) {
        MavenRepoSearcher searcher = new MavenRepoSearcher();
        searcher.showRepoListWindow(searcher.searchRepo("common io"));
    }

}
