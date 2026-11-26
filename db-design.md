# DB設計書（やること管理アプリ）

## 1. 目的と前提

この設計書は、`requirements.md` と `handout_app_spec.md` に基づき、アプリがMySQLへ保存するデータの形を定めます。テーブルは `users` と `todos` とし、ToDoを作成者のユーザーに紐付けます。画面の配置や表示方法は画面設計書で定めます。

**用語**：カラム（列）は、表の中で1種類の値を入れる場所です。NULL（ヌル）は「値が入っていない」状態です。主キーは、各行を重複なく見分ける番号です。

## 2. テーブル一覧

| テーブル名 | 用途 | 行の単位 |
|---|---|---|
| `users` | ログイン情報を保存する | ユーザー1人 |
| `todos` | 作成者に紐付けたやることを保存する | やること1件 |

## 3. `todos` カラム定義

| カラム | 型 | NULL | 既定値・自動設定 | 内容・制約 |
|---|---|---|---|---|
| `id` | `BIGINT` | 不可 | `AUTO_INCREMENT`（保存時に番号を自動採番） | 主キー。行を識別する番号 |
| `user_id` | `BIGINT` | 不可 | ログインユーザーから自動設定 | users.idへの外部キー。所有者別インデックスあり |
| `title` | `VARCHAR(255)` | 不可 | なし | やること。空白だけはアプリ側の入力チェックで拒否し、255文字以内とする |
| `detail` | `VARCHAR(255)` | 可 | なし | メモ。任意、255文字以内 |
| `category` | `VARCHAR(255)` | 不可 | なし | ジャンル。アプリ側で「デザイン」「マーケティング」「プログラミング」「資格」「就職活動」「その他」のいずれかを保存 |
| `priority` | `INT` | 不可 | `2` | 優先度。`1`＝高、`2`＝中、`3`＝低 |
| `due_date` | `DATE` | 可 | なし | 期限。任意。入力チェック上の制限なし |
| `completed` | `BOOLEAN` | 不可 | `FALSE`（未完了） | 完了状態。`TRUE`＝完了、`FALSE`＝未完了 |
| `created_at` | `DATETIME` | 不可 | `CURRENT_TIMESTAMP` | 登録日時。DBが自動設定 |
| `updated_at` | `DATETIME` | 不可 | `CURRENT_TIMESTAMP`、更新時に自動更新 | 最終更新日時。DBが自動設定 |

`BOOLEAN` はMySQLでは真偽値を表す型で、`TINYINT(1)` として扱われます。DBにはアプリで扱いやすい `BOOLEAN` と記述します。

## 4. CREATE TABLE 文（DDL）

DDL（テーブルを作成するためのSQL文）は次のとおりです。

```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS todos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    detail VARCHAR(255),
    category VARCHAR(255) NOT NULL,
    priority INT NOT NULL DEFAULT 2,
    due_date DATE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_todos_user_id (user_id),
    CONSTRAINT fk_todos_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

`utf8mb4` は日本語を含む文字を保存できる文字コードです。ジャンルの選択肢、優先度の範囲、必須項目の空白判定はアプリ側でも確認します。`updated_at` は行を更新するたびにMySQLが現在日時へ更新します。

## 5. 要件とカラムの対応

| 要件（`requirements.md`） | 対応カラム | 対応内容 |
|---|---|---|
| 4.1 一覧表示 | `title`, `category`, `priority`, `due_date`, `completed` | 一覧に表示する値を保存。優先度と完了状態は保存値を画面用の言葉に読み替える |
| 4.2 新規登録 | `title`, `detail`, `category`, `priority`, `due_date` | 登録時に入力する5項目を保存。メモと期限はNULL可 |
| 4.3 編集 | `title`, `detail`, `category`, `priority`, `due_date`, `completed` | 登録内容と完了状態を更新する |
| 4.4 削除 | `id`（対象の特定） | 指定された行を削除する |
| 4.5 名前で絞り込み | `title` | 入力語を含む行を検索する（部分一致） |
| 4.5 ジャンルで絞り込み | `category` | 選択ジャンルと一致する行を検索する（完全一致） |
| 4.5 期限で並び替え | `due_date` | 期限の近い順／遠い順に並べる。初期表示は近い順 |
| 4.6 入力チェック | `title`, `detail`, `category`, `priority`, `due_date` | 必須・最大255文字・選択肢の確認はアプリ側で行う。期限は任意で制限なし |
| 4.7 存在しない番号の確認 | `id` | 編集・削除時に対象行があるか確認する |
| 5 データ要件 | 全カラム | MySQLの `todos` に作成者と紐付けて1件1行で保存。`id` と日時は利用者入力でなく自動設定 |

## 6. 保存対象外と操作記録

- 一覧の絞り込み条件や並び順はURLで受け渡す条件であり、テーブルには保存しません。
- 「登録しました」などの操作結果メッセージも、行データとして保存しません。
- 要件4.8の操作記録は、操作種類と対象 `id` のみを記録し、`title` と `detail` を含めません。操作記録用テーブルは追加しません。記録はアプリケーションのログ（アプリの動作記録）へ出力します。
- 要件で対象外とされた完了日時、添付ファイル、削除済みデータはカラムとして追加しません。

## ユーザー別アクセス制御と移行

一覧・検索件数・更新・削除はログインユーザーのuser_idで制限します。他ユーザーのToDoへのアクセスは403です。既存共有データの移行手順はAUTHENTICATION.mdを参照してください。
