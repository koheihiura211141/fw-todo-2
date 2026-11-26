# ログインとユーザー別ToDo管理

ToDoの利用には登録・ログインが必要です。ゲストログインは廃止しました。
ユーザー登録時のロールはUSER、パスワードはBCryptで保存します。

- 画面への未ログインアクセスは `/login` へ移動します。API・MCPは401です。
- 一覧・検索・件数・カレンダー用APIはログインユーザーのToDoだけを返します。
- 作成者IDはDBのユーザーIDを持つ `LoginUser` から自動設定します。入力値で所有者を変更できません。
- 詳細・編集確認・更新・完了切替・削除で所有者を確認し、他ユーザーのToDoなら403です。
- 一括削除に他ユーザーのToDoが含まれる場合、1件も削除せず403にします。
- Serviceでも認証を確認するため、MCPからの呼び出しにも同じ制限が適用されます。
- 更新・削除SQLにもuser_id条件を付けています。CSRF保護は有効です。

## DBの準備

新規DBでは起動時の `schema.sql` がusersとtodosを作成します。
user_idはNOT NULL・外部キー付きで、所有者別検索用インデックスがあります。

既存DBのCREATE TABLE IF NOT EXISTSでは列は追加されません。バックアップを取り、アプリを停止して次の順で移行してください。

1. `src/main/resources/db/manual/01-add-todo-owner.sql` を一度実行する。
2. 既存ToDoの所有者を確認し、各行のuser_idを実在するusers.idに明示的に割り当てる。以前は共有データだったため、自動割り当ては行わない。
3. `SELECT id FROM todos WHERE user_id IS NULL` が0件であることを確認する。
4. `src/main/resources/db/manual/02-require-todo-owner.sql` を実行してNOT NULL制約を付ける。
5. アプリを再起動し、再ログインする。旧ゲストセッションはアクセス不可。

## 初回ログインのサンプル

ログイン成功時に、ユーザーごとに一度だけ15件のサンプルToDoを追加します（10件＋5件の2ページ）。
全6ジャンル・優先度3段階・完了/未完了・期限切れ/当日/未来/期限なしを含みます。
期限昇順の1ページ目は、期限切れ1件・3日以内2件・通常7件で、各表示を比較できます。
「ポートフォリオ」「デザイン」などの名前検索や、各列の並び替え・条件の組み合わせを試せます。
期限は初回ログイン日の日本時間を基準に設定します。
サンプルは通常のToDoと同様に編集・削除できます。全件削除しても再ログイン時に復活しません。
既存アカウントにも、この機能の導入後の初回ログイン時に追加されます。
起動時のschema.sqlが初期化履歴テーブルsample_todo_initializationsを追加するため、既存usersへの列追加は不要です。
履歴と15件の追加は同一トランザクションで行い、同時ログインはユーザー行のロックで直列化します。

## テスト

`mvnw.cmd test` でテストします。TodoappApplicationTestsの起動にはMySQLが必要です。
DB不要の検証は `mvnw.cmd -Dtest=AuthenticationTests,TodoOwnershipTests,TodoBulkDeleteTests,TodoFilterTests,TodoDeadlineTests,QuickTodoTests test` です。

提出用には個別のデモアカウントを用意し、別ユーザーのToDoへアクセスすると403になる確認手順を添えてください。
