・res/drawable-xdpi/ic_launcher.pngに96x96のアイコンを置く
・res/drawable-hdpi/ic_launcher.pngに72x72のアイコンを置く
・res/drawable-mdpi/ic_launcher.pngに48x48のアイコンを置く
・libsにtwitter4jのライブラリjar(3.0.3)を入れてパスを通す
・sabikoi.app.coiltwitterにクラスCosumerKeysを定義してコールバックURL、コンシューマキー等を定義(またはMainActivityを書き換え)

すれば動くはず。ビルド要求はとりあえずAndroid 2.1
現在できることは

・OAuth認証
・任意タイミングでのTL更新
・UserStreamの切り替え
・post/画像付きpost(twitter公式)
