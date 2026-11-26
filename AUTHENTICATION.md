# ログイン・ゲスト利用

MySQLを起動してからアプリを起動し、`http://localhost:8081/login` を開きます。
`users` テーブルは起動時に `schema.sql` で作成します（既存テーブルは削除しません）。

- 「アカウントを作成」からユーザー名とパスワードを登録します。既定のアカウントや共通パスワードはありません。
- パスワードはBCryptでハッシュ化し、登録時のロールは常に `USER` です。
- 「ゲストとして利用」を押すと、ユーザー登録やDBへのユーザー追加なしで `GUEST` のセッションを開始します。
- 未認証で画面・API・MCPにアクセスするとログイン画面に移動します。静的リソースは公開しています。
- ログアウトはCSRFトークン付きPOSTで実行し、セッションを破棄します。

## ToDoの共有範囲

既存の共有ToDo方式を維持しています。登録ユーザーもゲストも同じToDoを参照・追加・更新・削除できます。
ゲストのToDoもDBに保存され、ログアウト時には削除されません。ユーザーごとの非公開ToDo機能は含みません。

## 依存関係

Spring Boot 4.0.9 が管理するSpring Security 7.xを使用します。Spring Security 6.xへの強制ダウングレードは行いません。
Thymeleaf連携には `thymeleaf-extras-springsecurity6` を使用します。
https://spring.io/guides/gs/securing-web/

## テスト

`mvnw.cmd test` で実行します。全体の起動テストには `localhost:3307` のMySQLが必要です。
認証の単体結合テストのみの場合は `mvnw.cmd -Dtest=AuthenticationTests test` を実行します。
