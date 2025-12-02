# ShannonUIMod アーキテクチャ分析

## 📋 概要

ShannonUIModは**Minecraft Fabric Mod**で、Shannonボット（backend）とMinecraftクライアント間のUIブリッジとして機能します。

### 目的
1. **BackendからのデータをMinecraftクライアントに表示**
   - タスクツリー（現在の実行タスク）
   - 詳細ログ（Cursor AIスタイル）
   - スキル状態
   - インベントリ
   - チャット

2. **クライアントからBackendへの操作**
   - アイテム投げ捨て
   - スキルのオン/オフ
   - チャットメッセージ送信

---

## 🏗️ 現在のアーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                    ShannonUIMod (Fabric Mod)                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐         ┌──────────────┐                 │
│  │   Server    │◄────────┤   Backend    │                 │
│  │   (Main)    │  HTTP   │  (Node.js)   │                 │
│  │  Port 8081  │◄────────┤  Port 8082   │                 │
│  └──────┬──────┘         └──────────────┘                 │
│         │                                                   │
│         │ Packet (S2C/C2S)                                 │
│         │                                                   │
│  ┌──────▼──────┐                                           │
│  │   Client    │                                           │
│  │ (Rendering) │                                           │
│  └─────────────┘                                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📂 ディレクトリ構造

### Server Side (`src/main/java/com/shannon/`)

```
com/shannon/
├── ShannonUIMod.java              ← メインエントリーポイント (238行)
├── MyModServer.java               ← シンプルなイベント登録 (21行)
│
├── state/
│   └── StateManager.java          ← 状態管理の中央クラス (211行)
│
├── http/
│   ├── HttpServerManager.java     ← HTTPサーバー管理 (81行)
│   └── endpoints/                 ← 各HTTPエンドポイント
│       ├── TaskEndpoint.java              (52行)
│       ├── TaskLogsEndpoint.java          
│       ├── ConstantSkillsEndpoint.java    
│       ├── ChatEndpoint.java              
│       ├── InventoryClickEndpoint.java    
│       ├── ConstantSkillClickEndpoint.java
│       └── ChatMessageEndpoint.java       
│
├── network/packet/                ← パケット定義（S2C/C2S通信）
│   ├── TaskTreeState.java         ← 状態データクラス
│   ├── TaskTreeStatePacket.java   ← パケットラッパー
│   ├── DetailedLogsState.java
│   ├── DetailedLogsStatePacket.java
│   ├── InventoryState.java
│   ├── InventoryStatePacket.java
│   ├── ConstantSkillsState.java
│   ├── ConstantSkillsStatePacket.java
│   ├── ChatState.java
│   ├── ChatStatePacket.java
│   ├── PlayerStatusState.java
│   ├── PlayerStatusStatePacket.java
│   ├── InventoryItemClickPacket.java      ← C2S
│   ├── ConstantSkillClickPacket.java      ← C2S
│   ├── ChatMessageSendPacket.java         ← C2S
│   ├── MessagePacket.java
│   └── LogToggleState.java
│
├── mixin/                         ← Minecraftコアへの介入
│   ├── ServerPlayerEntityMixin.java
│   ├── PlayerInventoryMixin.java
│   └── PlayerStatusMixin.java
│
└── util/
    └── InventoryStateUtil.java    ← インベントリ状態作成
```

### Client Side (`src/client/java/com/shannon/`)

```
com/shannon/
├── ShannonUIModClient.java        ← クライアントエントリーポイント (263行)
├── ShannonUIScreen.java           ← フルスクリーンUI
├── UIRenderer.java                ← 基底UIレンダラー
│
└── 各UIレンダラー:
    ├── TaskTreeUIRenderer.java
    ├── DetailedLogsUIRenderer.java
    ├── InventoryUIRenderer.java
    ├── ConstantSkillsUIRenderer.java
    ├── ChatUIRenderer.java
    └── PlayerStatusRenderer.java
```

---

## 🔄 データフロー

### 1. Backend → Client (情報表示)

```
Backend (Node.js)
  │
  │ HTTP POST
  ▼
HttpServer (Port 8081)
  │
  │ /task, /task_logs, /constant_skills, /chat
  ▼
HttpEndpoints (各エンドポイント)
  │
  │ JSON解析
  ▼
StateManager
  │
  │ 状態更新 + ブロードキャスト
  ▼
Fabric Networking (S2C Packet)
  │
  │ TaskTreeStatePacket, DetailedLogsStatePacket, etc.
  ▼
Client
  │
  │ パケット受信 → 状態更新
  ▼
UIRenderers
  │
  │ HUDまたはScreenに描画
  ▼
Minecraft Client画面
```

### 2. Client → Backend (操作)

```
Minecraft Client
  │
  │ マウスクリック、キー入力
  ▼
UIRenderers
  │
  │ イベント検出
  ▼
Fabric Networking (C2S Packet)
  │
  │ InventoryItemClickPacket, ConstantSkillClickPacket, etc.
  ▼
ShannonUIMod (Server)
  │
  │ パケット受信ハンドラ
  ▼
HTTP Client
  │
  │ HTTP POST → Backend (Port 8082)
  ▼
Backend (Node.js)
  │
  │ /throw_item, /constant_skill_switch, /chat_message
  ▼
Minebot処理
```

---

## 🎯 各クラスの責任

### Server Side

#### `ShannonUIMod.java` (238行)
**責任**: 
- Mod初期化
- パケット登録（S2C/C2S）
- C2Sパケット受信ハンドラ
- HTTPサーバー起動
- ユーティリティメソッド

**問題点**:
❌ 責任が過多（初期化 + パケット処理 + HTTP通信 + ユーティリティ）
❌ C2Sハンドラ内でHTTP通信ロジックがハードコード（58-141行）
❌ ユーティリティメソッドが静的で、凝集性が低い

#### `StateManager.java` (211行)
**責任**: 
- 全状態の一元管理
- 状態更新時のブロードキャスト
- リスナー管理（将来拡張用）

**良い点**:
✅ 状態管理が一元化
✅ 更新通知パターン実装済み
✅ シングルトンで一貫性確保

**改善可能**:
⚠️ ブロードキャストロジックが繰り返し（DRY違反）
⚠️ ジェネリクスを使えばより型安全に

#### `HttpServerManager.java` (81行)
**責任**: 
- HTTPサーバーの起動/停止
- エンドポイント登録

**良い点**:
✅ HTTPサーバー管理が一元化
✅ エンドポイント追加が容易

**改善可能**:
⚠️ 設定値（PORT）がハードコード
⚠️ スレッドプールサイズが固定

#### `HttpEndpoints` (各50-60行)
**責任**: 
- 特定のHTTPエンドポイント処理
- JSON解析
- StateManager更新

**良い点**:
✅ 各エンドポイントが独立
✅ HttpHandlerインターフェース実装

**改善可能**:
⚠️ エラーハンドリングが各クラスで重複
⚠️ JSON解析の共通処理を抽出可能

### Client Side

#### `ShannonUIModClient.java` (263行)
**責任**: 
- クライアント初期化
- パケット受信ハンドラ登録
- キーバインディング
- 状態管理
- UI描画の調整

**問題点**:
❌ 責任が過多（パケット受信 + 状態管理 + キー処理 + UI調整）
❌ 各UIRendererへの委譲が冗長
❌ 状態が分散（taskTreeState, inventoryState, etc.）

#### `UIRenderer系` (各100-300行)
**責任**: 
- 特定のUI要素の描画
- マウス/キーボードイベント処理

**良い点**:
✅ UIが機能ごとに分離
✅ 描画ロジックがカプセル化

**改善可能**:
⚠️ 共通の描画処理を基底クラスに
⚠️ イベント処理の統一

---

## ⚠️ 主な問題点

### 1. **責任の分散と重複**
- `ShannonUIMod.java`が大きすぎる（238行）
- C2Sパケットハンドラ内のHTTP通信コードが重複
- エラーハンドリングが各所で重複

### 2. **ハードコード**
- ポート番号（8081, 8082）
- URL構築が直書き
- エンドポイント名が文字列リテラル

### 3. **型安全性**
- パケット定義が手動
- JSON解析で型情報が失われる箇所

### 4. **テスタビリティ**
- 静的メソッドが多い
- 依存性注入がない
- モックが困難

### 5. **エラーハンドリング**
- try-catchが各所で重複
- エラーログが統一されていない
- ユーザーへのフィードバックが不十分

---

## 🎨 やろうとしていること（意図）

### 1. **リアルタイム情報表示**
Backendで実行中のタスクをMinecraftクライアントにリアルタイム表示

### 2. **双方向操作**
クライアントからBackendのボットを操作可能に

### 3. **状態同期**
Server-Client間で状態を同期し、複数プレイヤーが同じ情報を共有

### 4. **拡張性**
新しいUI要素や機能を簡単に追加できる設計

---

## 🚀 次のステップ

Phase 7として以下のリファクタリングを提案：

1. **設定の一元化** (MinecraftConfig.java)
2. **HTTP通信の抽象化** (BackendClient.java)
3. **パケット処理の統一** (PacketHandlerRegistry.java)
4. **エラーハンドリング統一** (ModErrorHandler.java)
5. **Client状態管理の改善** (ClientStateManager.java)
6. **型安全性の向上** (interface定義)

