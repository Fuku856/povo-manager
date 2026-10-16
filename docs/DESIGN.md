---
name: povo Manager site
colors:
  ground: "#F3F5F6"
  surface: "#E6EAEC"
  ink: "#1A1C1E"
  ink-secondary: "#4C5359"
  accent: "#00658E"
  field: "#E6EAEC"
  field-dark: "#1A1C1E"
  field-ink: "#1A1C1E"
  field-ink-dark: "#E8EAEC"
  sim: "#4FC3F7"
  safe-container: "#D7F2DC"
  safe-content: "#1B5E2A"
  warning-container: "#FBEFC9"
  warning-content: "#6D5605"
  alert-container: "#FCE3C8"
  alert-content: "#8A4B00"
  danger-container: "#FFDAD6"
  danger-content: "#8C1D18"
typography:
  display:
    fontFamily: LINE Seed JP
    fontSize: 56px
    fontWeight: 800
  h2:
    fontFamily: LINE Seed JP
    fontSize: 42px
    fontWeight: 800
  number:
    fontFamily: LINE Seed JP
    fontSize: 168px
    fontWeight: 800
  body:
    fontFamily: Hiragino Sans
    fontSize: 17px
    fontWeight: 400
  small:
    fontFamily: Hiragino Sans
    fontSize: 14px
    fontWeight: 400
rounded:
  chip: 10px
  card: 20px
  panel: 28px
  button: 999px
spacing:
  gutter: 16px
  gutter-wide: 40px
  section: 176px
  section-phone: 88px
---

# DESIGN.md — povo Manager 紹介ページ

> `docs/index.html` 用のデザイン仕様。トークンの値は `index.html` の `:root` と同じ。
> 書式は google-labs-code/design.md、日本語の組版ルールは kzhrknt/awesome-design-md-jp のテンプレートに沿っている。

## 1. Visual Theme & Atmosphere

- **方針**: アプリの見た目をそのまま持ってくる。新しい装飾は足さない
- **主役**: 「あと◯日」の数字。アプリのバッジと進捗バーと同じ色の変わり方をページでも使う
- **地**: ライトでは明るいグレーの地に、少し濃いグレー `#E6EAEC` を面（ヒーロー、インストール、フッター）として敷く。ダークではアプリアイコンの背景色 `#1A1C1E` を面にする。ライトの中に暗い面を混ぜない

## 2. Color Palette & Roles

- **Field**: ヒーローとインストールとフッターの面。ライトは `#E6EAEC`（文字 `#1A1C1E`）、ダークは `#1A1C1E`（文字 `#E8EAEC`、見出しは白）。`theme-color` もモードごとに合わせる
- **SIM** (`#4FC3F7`): アイコンの SIM カードの色。暗い面の上のボタンとフォーカス枠だけに使う
- **Accent** (`#00658E`): アプリの primary。明るい面の上のリンクとボタン
- **残日数の4色**: アプリの `UrgencyPalette`（`ui/common/StatusUi.kt`）と同じ値。31日以上は緑、30日以内は黄、14日以内は橙、7日以内と期限切れは赤
- **Ink** (`#1A1C1E`): 本文。純黒は使わない。本文を灰色にしない（灰色は補足だけ）
- ダークモードは `prefers-color-scheme` で切り替え、残日数の色もアプリのダーク版に合わせる

## 3. Typography Rules

### 3.1 書体

- **見出しと数字**: LINE Seed JP 700 / 800（Google Fonts）。約物は Yaku Han JP を先頭に置いて半角幅にする
- **本文**: OS の標準ゴシック。Webフォントを読み込まないので軽い

```css
--font-display: "YakuHanJP", "LINE Seed JP", "Hiragino Sans", "Hiragino Kaku Gothic ProN", "Noto Sans JP", Meiryo, sans-serif;
--font-text: "Hiragino Sans", "Hiragino Kaku Gothic ProN", "Noto Sans JP", "BIZ UDPGothic", Meiryo, sans-serif;
```

### 3.2 大きさ

| Role | Size | Weight | Line Height | Letter Spacing |
|---|---|---|---|---|
| 残り日数の数字 | 96–168px | 800 | 0.9 | -0.04em |
| h1 | 34–56px | 800 | 1.25 | -0.01em |
| h2 | 27–42px | 800 | 1.25 | -0.01em |
| 本文 | 17px | 400 | 1.85 | 0.03em |
| 補足 | 14px | 400 | 1.7 | 0.03em |

### 3.3 改行と字詰め

- 見出しは **どの幅でも2行まで**。文節の切れ目に `<wbr>` を入れ、`word-break: auto-phrase`（未対応ブラウザは `keep-all`）と `text-wrap: balance` で折り返す
- 見出しだけ `font-feature-settings: "palt"`。本文には使わない
- `line-break: strict` で句読点の行頭を禁止

## 4. Component Stylings

- **ボタン**: 完全な丸（`999px`）。影もグラデーションも付けない。押すと 0.97 倍に縮む
- **計算機**: アプリの回線カードをそのまま大きくしたもの。カード全体が残日数の色になる
- **チップ**: 角丸 10px の塗りだけ。線で囲まない
- **スクショの枠**: `#1A1C1E` の角丸。端末の枠なので、ライトでも暗いまま。iPhone 型の枠は使わない
- **ヘッダー**: 最上部では全幅の Field 色のバー。スクロールすると上に残ったまま、幅 960px までのピル（角丸 `999px`）に変わる。ピルは Field 色 64% の半透明＋ぼかし（`backdrop-filter: blur(12px)`）で後ろが少し見える。文字色 14% の 1px の枠を付け、ナビの文字は読めるように明るい色（`--field-ink`）にする。ぼかしが使えないブラウザでは不透明にする。JS が無いときは全幅のバーのまま。画面の真ん中を過ぎたセクション（ページの一番下では「質問」）のリンクには、文字色 10% の角丸 `999px` の面を敷く（通知・ウィジェット・トッピング・そのほかは「できること」に含める）。リンクを押すとなめらかにスクロールする。ロゴはどの幅でも最上部に戻る。幅 961px 以上では、計算機がヒーローの横にあるので「残り日数」も最上部に戻る
- **メニュー**: 960px 以下ではナビのリンクをメニューボタンの中に入れる。開くとヘッダーの下に角丸 28px のパネル（Field 色 92% の半透明＋ぼかし）が出る。リンク、Esc、パネルの外を押すと閉じる
- **最新版の情報**: ヒーローとインストールに、バージョン、公開日、`MIT License` をチップで並べる。中黒でつながず、1つずつ文字色 8% の塗りのチップにする。バージョンと公開日は GitHub API から取れたときだけ出す
- **戻るボタン**: ヒーローが見えなくなったら右下に出る、直径 48px の丸。一段明るい面（`--field-raise`）に文字色 50% の 2px の枠を付け、どの面の上でも地とのコントラストを 3:1 以上にする。矢印は太め（2.4）。影は付けない
- **インストール**: 「APKを直接入れる」と「Obtainium で自動更新」の2つを、Field の面の上に一段明るい角丸 20px の区画（ライトは `#FAFBFB`、ダークは `#232628`）で横に並べる。2つめのボタンは枠線だけ（`.btn.outline`）

## 5. Layout Principles

- 左揃えが基本。中央揃えは使わない
- セクション間は 88–176px。セクション内の余白より必ず大きくする
- 隣り合うセクションは違う組み方にする（左右入れ替え、面の色を変える、表を置く）

## 6. Depth & Elevation

影は使わない。面どうしは色の段差で分ける。ヘッダーのピルとメニューだけは、下の内容の上に重なるので半透明＋ぼかしと 1px の枠で分ける。

### 動き

- 動かすのは形と位置だけ。文字の色や大きさは動かさず、読んでいる最中の本文は動かさない
- 長さは 0.15〜0.3s、曲線は `cubic-bezier(.2,.7,.2,1)`
- 使っている動き: ヘッダーのリンクと戻るボタンでのなめらかなスクロール、戻るボタンが下から 8px 上がりながら出る、見出しや段落が下から出てくる（`.rv`）、ヘッダーがピルに変わる、メニューが上から 8px 下りながら出てリンクが 30ms ずつ遅れて出る、メニューボタンの三本線が × になる、よくある質問の開け閉めで高さが変わる（`::details-content` と `interpolate-size`。未対応のブラウザではすぐ開く）、ボタンを押すと 0.97 倍に縮む
- `prefers-reduced-motion: reduce` では全部止める
- スマホでタップしたときの青いハイライトは出さない（`-webkit-tap-highlight-color: transparent`）。押した感触はボタンの縮みで出す

## 7. Do's and Don'ts

### Do

- 数字はアプリと同じ計算（最後の購入日 + 180日）で出す
- 本物のスクショか、アプリと同じ見た目の HTML を置く
- 文章は短く、言い切る

### Don't

- グラデーション、アイコン付きの3列カード
- ヘッダーのピルとメニュー以外での、ガラス風の半透明
- 見出しの上の小さい英字ラベル、01 / 02 のような番号、等幅フォントの飾り
- 絵文字、全角ダッシュ、中黒で3つ以上並べる書き方

## 8. Responsive Behavior

| Name | Width | 変わるところ |
|---|---|---|
| Narrow | < 360px | ヘッダーのロゴは文字を隠してアイコンだけにする |
| Phone | ≤ 860px | 2カラムを1カラムにする。スクショは文章の下に置く（通知・そのほかと同じ並び）。インストールの2択を縦に積む |
| Tablet | ≤ 960px | ヘッダーのリンクをメニューボタンの中に入れ、ダウンロードボタンとメニューボタンを残す |
| Desktop | > 960px | 2カラム |

タップできるものは 40px 以上の高さにする。
