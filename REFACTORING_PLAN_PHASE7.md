# ShannonUIMod Phase 7 リファクタリング計画

## 🎯 目標

Backend（Phase 6完了）と同様に、ShannonUIModも以下を達成：
- ✅ **単一責任の原則**
- ✅ **保守性の向上**
- ✅ **テスタビリティの向上**
- ✅ **設定の一元化**
- ✅ **エラーハンドリングの統一**

---

## 📋 Phase 7.1: 設定の一元化

### 現状の問題
- ポート番号がハードコード（8081, 8082）
- URL構築が各所で重複
- エンドポイント名が文字列リテラル

### 提案

#### `ModConfig.java` (新規)
```java
package com.shannon.config;

public class ModConfig {
    // Backend接続設定
    public static final String BACKEND_HOST = "localhost";
    public static final int BACKEND_PORT = 8082;
    public static final String BACKEND_BASE_URL = 
        "http://" + BACKEND_HOST + ":" + BACKEND_PORT;
    
    // HTTPサーバー設定
    public static final int HTTP_SERVER_PORT = 8081;
    public static final int HTTP_THREAD_POOL_SIZE = 4;
    
    // エンドポイント定義
    public static final String ENDPOINT_THROW_ITEM = "/throw_item";
    public static final String ENDPOINT_SKILL_SWITCH = "/constant_skill_switch";
    public static final String ENDPOINT_CHAT_MESSAGE = "/chat_message";
    
    // ターゲットプレイヤー
    public static final String TARGET_PLAYER_NAME = "I_am_Shannon";
    
    // UI設定
    public static final int UI_MARGIN = 10;
    public static final int UI_PADDING = 5;
    public static final int UI_LINE_HEIGHT = 12;
}
```

**影響ファイル**: 
- `ShannonUIMod.java` (ポート番号、プレイヤー名)
- `HttpServerManager.java` (ポート、スレッド数)
- C2Sパケットハンドラ（URL構築）

---

## 📋 Phase 7.2: HTTP通信の抽象化

### 現状の問題
- HTTP通信コードが各C2Sハンドラで重複
- エラーハンドリングが統一されていない
- URLやJSON構築が直書き

### 提案

#### `BackendClient.java` (新規)
```java
package com.shannon.client;

public class BackendClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendClient.class);
    
    /**
     * 汎用POSTリクエスト
     */
    public static void post(String endpoint, String jsonBody) {
        post(endpoint, jsonBody, null);
    }
    
    /**
     * POSTリクエスト（コールバック付き）
     */
    public static void post(String endpoint, String jsonBody, Consumer<Integer> callback) {
        try {
            URI uri = URI.create(ModConfig.BACKEND_BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            LOGGER.info("POST {} response: {}", endpoint, responseCode);
            
            if (callback != null) {
                callback.accept(responseCode);
            }
            
            conn.disconnect();
        } catch (Exception e) {
            LOGGER.error("POST {} 送信失敗: {}", endpoint, e.getMessage(), e);
            ModErrorHandler.handle(new BackendCommunicationException(endpoint, e));
        }
    }
    
    /**
     * JSONオブジェクトからPOST
     */
    public static void postJson(String endpoint, Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(data);
            post(endpoint, json);
        } catch (Exception e) {
            LOGGER.error("JSON変換失敗", e);
            ModErrorHandler.handle(new JsonSerializationException(e));
        }
    }
}
```

#### `BackendRequest.java` (新規) - 型安全なリクエストクラス
```java
package com.shannon.client.request;

public class ThrowItemRequest {
    private String itemName;
    
    public ThrowItemRequest(String itemName) {
        this.itemName = itemName;
    }
    
    public String getItemName() { return itemName; }
}

public class SkillSwitchRequest {
    private String skillName;
    private String status;
    
    public SkillSwitchRequest(String skillName, boolean status) {
        this.skillName = skillName;
        this.status = String.valueOf(status);
    }
}

public class ChatMessageRequest {
    private String sender;
    private String message;
    
    public ChatMessageRequest(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }
}
```

**使用例**:
```java
// Before (ShannonUIMod.java 58-75行)
String json = "{\"itemName\":\"" + itemName + "\"}";
URI uri = URI.create("http://localhost:8082/throw_item");
HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
// ... 重複コード ...

// After
BackendClient.postJson(
    ModConfig.ENDPOINT_THROW_ITEM,
    new ThrowItemRequest(itemName)
);
```

---

## 📋 Phase 7.3: パケット処理の統一

### 現状の問題
- パケットハンドラが `ShannonUIMod.onInitialize()` に直書き
- 各ハンドラのロジックが長い
- 新しいパケット追加時の手間

### 提案

#### `PacketHandlerRegistry.java` (新規)
```java
package com.shannon.network;

public class PacketHandlerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketHandlerRegistry.class);
    
    /**
     * 全てのC2Sパケットハンドラを登録
     */
    public static void registerC2SHandlers() {
        registerMessageHandler();
        registerInventoryClickHandler();
        registerSkillClickHandler();
        registerChatMessageHandler();
        LOGGER.info("全てのC2Sパケットハンドラを登録しました");
    }
    
    private static void registerMessageHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
            MessagePacket.PACKET_ID,
            (payload, context) -> {
                ServerPlayerEntity player = context.player();
                MyModServer.sendMessageToClient(player, payload.message());
            }
        );
    }
    
    private static void registerInventoryClickHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
            InventoryItemClickPacket.PACKET_ID,
            (payload, context) -> context.server().execute(() -> {
                BackendClient.postJson(
                    ModConfig.ENDPOINT_THROW_ITEM,
                    new ThrowItemRequest(payload.itemName())
                );
            })
        );
    }
    
    private static void registerSkillClickHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
            ConstantSkillClickPacket.PACKET_ID,
            (payload, context) -> context.server().execute(() -> {
                BackendClient.postJson(
                    ModConfig.ENDPOINT_SKILL_SWITCH,
                    new SkillSwitchRequest(payload.skillName(), payload.status())
                );
            })
        );
    }
    
    private static void registerChatMessageHandler() {
        ServerPlayNetworking.registerGlobalReceiver(
            ChatMessageSendPacket.PACKET_ID,
            (payload, context) -> {
                ServerPlayerEntity player = context.player();
                context.server().execute(() -> {
                    // StateManager更新
                    updateChatState(player, payload.message());
                    
                    // Backend通知
                    BackendClient.postJson(
                        ModConfig.ENDPOINT_CHAT_MESSAGE,
                        new ChatMessageRequest(
                            player.getName().getString(),
                            payload.message()
                        )
                    );
                });
            }
        );
    }
    
    private static void updateChatState(ServerPlayerEntity player, String message) {
        StateManager stateManager = ShannonUIMod.getStateManager();
        ChatState chatState = stateManager.getChatState();
        
        ChatState.ChatMessage chatMessage = new ChatState.ChatMessage();
        chatMessage.sender = player.getName().getString();
        chatMessage.message = message;
        chatMessage.timestamp = System.currentTimeMillis();
        chatState.messages.add(chatMessage);
        
        stateManager.updateChatState(chatState);
    }
}
```

#### `PacketRegistry.java` (新規)
```java
package com.shannon.network;

public class PacketRegistry {
    /**
     * 全てのパケットタイプを登録
     */
    public static void registerAll() {
        registerS2CPackets();
        registerC2SPackets();
    }
    
    private static void registerS2CPackets() {
        PayloadTypeRegistry.playS2C().register(MessagePacket.PACKET_ID, MessagePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(TaskTreeStatePacket.PACKET_ID, TaskTreeStatePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(InventoryStatePacket.PACKET_ID, InventoryStatePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ConstantSkillsStatePacket.PACKET_ID, ConstantSkillsStatePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerStatusStatePacket.PACKET_ID, PlayerStatusStatePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ChatStatePacket.PACKET_ID, ChatStatePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(DetailedLogsStatePacket.PACKET_ID, DetailedLogsStatePacket.PACKET_CODEC);
    }
    
    private static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(MessagePacket.PACKET_ID, MessagePacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(InventoryItemClickPacket.PACKET_ID, InventoryItemClickPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ConstantSkillClickPacket.PACKET_ID, ConstantSkillClickPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ChatMessageSendPacket.PACKET_ID, ChatMessageSendPacket.PACKET_CODEC);
    }
}
```

---

## 📋 Phase 7.4: エラーハンドリングの統一

### 提案

#### `ModErrorHandler.java` (新規)
```java
package com.shannon.error;

public class ModErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModErrorHandler.class);
    
    public static void handle(ModException e) {
        // ログ出力
        LOGGER.error("🚨 Error: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        
        // JSON形式で詳細出力
        LOGGER.error(e.toJson());
        
        // 通知（将来的に）
        // notifyPlayer(e);
    }
    
    public static void handleSilent(Exception e) {
        LOGGER.warn("⚠️ Warning: {}", e.getMessage());
    }
}
```

#### `ModException.java` (新規) - カスタム例外
```java
package com.shannon.error;

public abstract class ModException extends Exception {
    private final String errorCode;
    private final Map<String, Object> metadata;
    
    public ModException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
    }
    
    public String toJson() {
        // JSON形式で出力
    }
}

public class BackendCommunicationException extends ModException {
    public BackendCommunicationException(String endpoint, Throwable cause) {
        super("Backend通信失敗: " + endpoint, "BACKEND_COMM_ERROR");
        this.metadata.put("endpoint", endpoint);
        this.metadata.put("cause", cause.getMessage());
    }
}

public class StateUpdateException extends ModException { ... }
public class PacketHandlingException extends ModException { ... }
public class JsonSerializationException extends ModException { ... }
```

---

## 📋 Phase 7.5: Client状態管理の改善

### 現状の問題
- `ShannonUIModClient` に状態が分散
- 各UIRendererが直接状態にアクセス

### 提案

#### `ClientStateManager.java` (新規)
```java
package com.shannon.client.state;

public class ClientStateManager {
    private static ClientStateManager instance;
    
    private TaskTreeState taskTreeState;
    private InventoryState inventoryState;
    private ConstantSkillsState skillsState;
    private PlayerStatusState playerStatusState;
    private ChatState chatState;
    private DetailedLogsState logsState;
    private LogToggleState logToggleState = new LogToggleState();
    
    // リスナー
    private List<StateChangeListener> listeners = new ArrayList<>();
    
    public interface StateChangeListener {
        void onStateChanged(StateType type);
    }
    
    public enum StateType {
        TASK_TREE, INVENTORY, SKILLS, PLAYER_STATUS, CHAT, LOGS
    }
    
    // Singleton
    public static ClientStateManager getInstance() {
        if (instance == null) {
            instance = new ClientStateManager();
        }
        return instance;
    }
    
    // 状態更新メソッド（通知付き）
    public void updateTaskTree(TaskTreeState state) {
        this.taskTreeState = state;
        notifyListeners(StateType.TASK_TREE);
    }
    
    // ... 他の状態更新メソッド ...
    
    // リスナー管理
    public void addListener(StateChangeListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners(StateType type) {
        listeners.forEach(l -> l.onStateChanged(type));
    }
}
```

---

## 📋 Phase 7.6: ShannonUIMod.javaの簡素化

### Before (238行)
```java
public class ShannonUIMod implements ModInitializer {
    // 初期化
    // パケット登録（長い）
    // C2Sハンドラ（長い、重複）
    // HTTPサーバー起動
    // ユーティリティメソッド
}
```

### After (予想: 60行程度)
```java
public class ShannonUIMod implements ModInitializer {
    public static final String MOD_ID = "shannonuimod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static final StateManager stateManager = StateManager.getInstance();
    
    @Override
    public void onInitialize() {
        LOGGER.info("ShannonUIMod initializing...");
        
        // パケット登録
        PacketRegistry.registerAll();
        
        // ハンドラ登録
        PacketHandlerRegistry.registerC2SHandlers();
        
        // サーバーインスタンスをセット
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            stateManager.setServer(server);
            LOGGER.info("StateManager initialized");
        });
        
        // HTTPサーバー起動
        HttpServerManager.startServer();
        
        // イベント登録
        MyModServer.registerEvents();
        
        LOGGER.info("ShannonUIMod initialized successfully!");
    }
    
    public static StateManager getStateManager() {
        return stateManager;
    }
    
    // ユーティリティメソッドをPlayerUtilに移動
}
```

---

## 📊 リファクタリング効果

### コード量
- **Before**: `ShannonUIMod.java` (238行)
- **After**: 
  - `ShannonUIMod.java` (60行)
  - `ModConfig.java` (40行)
  - `BackendClient.java` (80行)
  - `PacketRegistry.java` (30行)
  - `PacketHandlerRegistry.java` (100行)
  - `ModErrorHandler.java` (50行)
  - **合計**: 360行（再利用可能な部品に分解）

### 品質向上
- ✅ **単一責任**: 各クラスが明確な役割
- ✅ **DRY**: 重複コード削除
- ✅ **型安全**: リクエストクラスで型保証
- ✅ **テスタビリティ**: モック可能
- ✅ **保守性**: 変更箇所が明確

---

## 🗂️ 新しいディレクトリ構造

```
src/main/java/com/shannon/
├── ShannonUIMod.java                 ← 簡素化 (60行)
├── MyModServer.java                  ← そのまま
│
├── config/                           ← NEW
│   └── ModConfig.java
│
├── client/                           ← NEW
│   ├── BackendClient.java
│   └── request/
│       ├── ThrowItemRequest.java
│       ├── SkillSwitchRequest.java
│       └── ChatMessageRequest.java
│
├── error/                            ← NEW
│   ├── ModErrorHandler.java
│   └── exceptions/
│       ├── ModException.java
│       ├── BackendCommunicationException.java
│       ├── StateUpdateException.java
│       └── PacketHandlingException.java
│
├── network/                          
│   ├── PacketRegistry.java           ← NEW
│   ├── PacketHandlerRegistry.java    ← NEW
│   └── packet/                       ← そのまま
│
├── state/
│   ├── StateManager.java             ← 改善
│   └── PlayerUtil.java               ← NEW (ユーティリティ移動)
│
├── http/
│   ├── HttpServerManager.java        ← 改善
│   └── endpoints/                    ← 改善（エラーハンドリング統一）
│
├── mixin/                            ← そのまま
└── util/                             ← そのまま
```

---

## 🚀 実装順序

### Step 1: Phase 7.1 - 設定の一元化
1. `ModConfig.java` 作成
2. `ShannonUIMod.java` で使用
3. `HttpServerManager.java` で使用

### Step 2: Phase 7.4 - エラーハンドリング
1. `ModErrorHandler.java` 作成
2. `ModException` 系作成

### Step 3: Phase 7.2 - HTTP通信抽象化
1. `BackendClient.java` 作成
2. リクエストクラス作成
3. C2Sハンドラで使用

### Step 4: Phase 7.3 - パケット処理統一
1. `PacketRegistry.java` 作成
2. `PacketHandlerRegistry.java` 作成
3. `ShannonUIMod.java` 簡素化

### Step 5: Phase 7.5 - Client状態管理
1. `ClientStateManager.java` 作成
2. `ShannonUIModClient.java` リファクタ

### Step 6: Phase 7.6 - 最終確認
1. 全体テスト
2. ドキュメント更新

---

## ✅ 完了条件

- [ ] ビルドエラーなし
- [ ] 全機能が正常動作
- [ ] コード重複率 < 5%
- [ ] 各クラスが100行以内（エンドポイント除く）
- [ ] エラーハンドリングが統一
- [ ] ログ出力が統一

