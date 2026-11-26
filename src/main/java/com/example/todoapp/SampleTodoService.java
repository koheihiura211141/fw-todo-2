package com.example.todoapp;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleTodoService {
    private final SampleTodoMapper samples;
    private final TodoMapper todos;

    public SampleTodoService(SampleTodoMapper samples, TodoMapper todos) {
        this.samples = samples;
        this.todos = todos;
    }

    @Transactional
    public void initialize(Long userId) {
        if (samples.lockUser(userId) == null) {
            throw new IllegalStateException("User not found");
        }
        if (samples.countInitialization(userId) > 0) return;

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        add(userId, today, "ポートフォリオの配色を見直す", "デザイン", 2, 3, false);
        add(userId, today, "Javaの例外処理を復習する", "プログラミング", 1, 0, false);
        add(userId, today, "応募企業の事業内容を調べる", "就職活動", 1, 6, false);
        add(userId, today, "基本情報技術者の過去問を解く", "資格", 2, 7, false);
        add(userId, today, "SNS投稿の反応を分析する", "マーケティング", 3, 9, true);
        add(userId, today, "作業フォルダを整理する", "その他", 3, null, false);
        add(userId, today, "ポートフォリオの紹介文を書く", "就職活動", 2, 5, false);
        add(userId, today, "ToDo検索APIのテストを書く", "プログラミング", 1, -1, false);
        add(userId, today, "バナーのデザイン案を作る", "デザイン", 3, 10, false);
        add(userId, today, "アクセス解析のレポートをまとめる", "マーケティング", 2, 8, false);
        add(userId, today, "資格試験の学習計画を立てる", "資格", 1, 11, true);
        add(userId, today, "面接で伝える強みを整理する", "就職活動", 1, 4, false);
        add(userId, today, "READMEにアプリの使い方を書く", "プログラミング", 2, 12, true);
        add(userId, today, "アイコンのデザインを統一する", "デザイン", 3, 14, false);
        add(userId, today, "今週のタスクを振り返る", "その他", 2, 13, true);
        samples.markInitialized(userId);
    }

    private void add(Long userId, LocalDate today, String title, String category,
                     int priority, Integer days, boolean completed) {
        Todo todo = new Todo();
        todo.setUserId(userId);
        todo.setTitle(title);
        todo.setDetail("【サンプル】" + switch (title) {
            case "ポートフォリオの配色を見直す" -> "期限は3日後。「3日以内」のアラートを確認できます。編集で配色のメモを追記してみましょう。";
            case "Javaの例外処理を復習する" -> "期限は今日・優先度は高です。状態ボタンで完了に切り替えてみましょう。";
            case "応募企業の事業内容を調べる" -> "ジャンル「就職活動」と優先度「高」を組み合わせて絞り込めます。";
            case "基本情報技術者の過去問を解く" -> "期限は1週間後です。カレンダーで表示し、期限のあるタスクを確認してみましょう。";
            case "SNS投稿の反応を分析する" -> "完了済みのサンプルです。状態「完了」で絞り込んでみましょう。";
            case "作業フォルダを整理する" -> "期限なしのサンプルです。期限の並び替えで表示位置を確認できます。";
            case "ポートフォリオの紹介文を書く" -> "名前に「ポートフォリオ」と入力すると、ジャンルの異なる2件を検索できます。";
            case "ToDo検索APIのテストを書く" -> "期限は昨日・未完了です。「期限切れ」のアラートを確認できます。";
            case "バナーのデザイン案を作る" -> "名前に「デザイン」と入力して検索できます。ジャンルの絞り込みとも比較してみましょう。";
            case "アクセス解析のレポートをまとめる" -> "期限は8日後です。期限の開始日・終了日を指定して絞り込んでみましょう。";
            case "資格試験の学習計画を立てる" -> "完了済み・優先度が高いサンプルです。状態や優先度の列で並び替えできます。";
            case "面接で伝える強みを整理する" -> "期限は4日後。「3日以内」のアラートが付くタスクとの違いを確認できます。";
            case "READMEにアプリの使い方を書く" -> "編集からタイトル・メモ・ジャンル・優先度・期限を変更できます。";
            case "アイコンのデザインを統一する" -> "期限は2週間後です。カレンダーや期限の降順で今後の予定を確認できます。";
            default -> "15件あるので一覧の2ページ目も確認できます。チェックボックスで複数選択して一括削除も試せます。";
        });
        todo.setCategory(category);
        todo.setPriority(priority);
        todo.setDeadline(days == null ? null : today.plusDays(days));
        todo.setCompleted(completed);
        todos.insert(todo);
    }
}
