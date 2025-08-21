# Basic REST Redis Example

這是一個使用 @DL (Domain Logic) 模式實現 Redis CRUD 操作的 Spring Boot 示範專案。

## 專案概述

此專案展示如何使用微服務框架的 @DL 註解模式來實現 Redis 資料存取操作，包括：

- **Create**: 建立新的 Redis 資料
- **Read**: 讀取 Redis 資料 (單一、分類、全部)
- **Update**: 更新現有的 Redis 資料 (完整更新或部分更新)
- **Delete**: 刪除 Redis 資料 (物理刪除或軟刪除/歸檔)

## 特色功能

- **@DL 領域邏輯模式**: 使用框架提供的 @DL 註解來組織業務邏輯
- **JSON 序列化**: 自動處理 Java 物件與 JSON 的轉換
- **分類管理**: 支援資料分類和索引
- **TTL 支援**: 支援資料過期時間設定
- **版本控制**: 樂觀鎖機制防止資料衝突
- **軟刪除**: 歸檔功能而非直接刪除
- **索引清理**: 自動維護資料一致性
- **REST API**: 提供完整的 HTTP 介面用於測試

## 專案結構

```
basic-rest-redis/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── blog/eric231/examples/basicrestredis/
│   │   │       ├── BasicRestRedisApplication.java       # 主應用程式
│   │   │       ├── model/
│   │   │       │   └── RedisData.java                   # 資料模型
│   │   │       ├── logic/
│   │   │       │   ├── RedisCreateLogic.java            # @DL 建立邏輯
│   │   │       │   ├── RedisReadLogic.java              # @DL 讀取邏輯
│   │   │       │   ├── RedisUpdateLogic.java            # @DL 更新邏輯
│   │   │       │   └── RedisDeleteLogic.java            # @DL 刪除邏輯
│   │   │       └── api/
│   │   │           └── RedisTestController.java         # REST 控制器
│   │   └── resources/
│   │       └── application.yml                          # 應用程式配置
│   └── test/                                            # 單元測試
├── pom.xml                                              # Maven 配置
└── README.md                                            # 說明文件
```

## 前置需求

- Java 17+
- Maven 3.6+
- Redis 伺服器 (本機或遠端)

## 快速開始

### 1. 啟動 Redis 伺服器

```bash
# 使用 Docker (推薦)
docker run -d --name redis-server -p 6379:6379 redis:latest

# 或使用本機 Redis
redis-server
```

### 2. 建置專案

```bash
mvn clean compile
```

### 3. 運行應用程式

```bash
mvn spring-boot:run
```

應用程式將在 `http://localhost:8083` 啟動。

## API 使用說明

### 健康檢查

```bash
curl -X GET http://localhost:8083/test/redis/health
```

### 建立資料

```bash
curl -X POST http://localhost:8083/test/redis/create \
  -H "Content-Type: application/json" \
  -d '{
    "id": "user-001",
    "name": "張三",
    "description": "系統管理員",
    "value": "admin",
    "category": "users",
    "ttlMinutes": 60,
    "metadata": {
      "department": "IT",
      "level": "senior"
    }
  }'
```

### 讀取資料

#### 單一資料查詢
```bash
curl -X GET http://localhost:8083/test/redis/user-001
```

#### 分類查詢
```bash
curl -X GET http://localhost:8083/test/redis/category/users
```

#### 複雜查詢
```bash
curl -X POST http://localhost:8083/test/redis/read \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "single",
    "id": "user-001",
    "includeMetadata": true
  }'
```

### 更新資料

#### 部分更新 (預設)
```bash
curl -X PUT http://localhost:8083/test/redis/update \
  -H "Content-Type: application/json" \
  -d '{
    "id": "user-001",
    "name": "張三 (已更新)",
    "metadata": {
      "level": "expert"
    }
  }'
```

#### 完整更新
```bash
curl -X PUT http://localhost:8083/test/redis/update \
  -H "Content-Type: application/json" \
  -d '{
    "id": "user-001",
    "operation": "update",
    "name": "張三",
    "description": "新的描述",
    "value": "新的值",
    "category": "administrators"
  }'
```

### 刪除資料

#### 軟刪除 (歸檔)
```bash
curl -X POST http://localhost:8083/test/redis/archive/user-001
```

#### 物理刪除
```bash
curl -X DELETE http://localhost:8083/test/redis/user-001
```

#### 分類刪除
```bash
curl -X DELETE http://localhost:8083/test/redis/delete \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "category",
    "category": "users",
    "force": true,
    "cleanup": true
  }'
```

### 取得操作說明

```bash
# 取得建立操作的說明
curl -X GET http://localhost:8083/test/redis/metadata/create

# 取得所有支援的操作
curl -X GET http://localhost:8083/test/redis/metadata/read
curl -X GET http://localhost:8083/test/redis/metadata/update
curl -X GET http://localhost:8083/test/redis/metadata/delete
```

## 配置說明

### Redis 連線配置 (application.yml)

```yaml
framework:
  redis:
    mode: standalone  # standalone 或 cluster
    standalone:
      host: localhost
      port: 6379
    database: 0
    timeout: 2000ms
    pool:
      max-total: 10
      max-idle: 8
      min-idle: 2
      max-wait: 3000ms
```

### 應用程式配置

```yaml
server:
  port: 8083

spring:
  application:
    name: basic-rest-redis

# 記錄配置
logging:
  level:
    blog.eric231.examples.basicrestredis: INFO
    blog.eric231.framework: INFO
```

## @DL 邏輯說明

### RedisCreateLogic

負責建立新的 Redis 資料，支援：
- ID 唯一性檢查
- 自動分類索引
- TTL 設定
- 中繼資料處理

### RedisReadLogic

負責讀取 Redis 資料，支援：
- 單一資料查詢
- 分類查詢
- 中繼資料包含選項

### RedisUpdateLogic

負責更新現有資料，支援：
- 部分更新 (patch)
- 完整更新 (update)
- 版本控制
- 分類變更處理

### RedisDeleteLogic

負責刪除資料，支援：
- 單一刪除
- 分類刪除  
- 軟刪除 (歸檔)
- 索引清理

## 資料模型

### RedisData

```java
{
  "id": "唯一識別碼",
  "name": "資料名稱",
  "description": "資料描述", 
  "value": "資料值",
  "category": "資料分類",
  "status": "active|inactive|archived",
  "version": "版本號碼",
  "createdAt": "建立時間",
  "updatedAt": "更新時間",
  "metadata": {
    "key1": "value1",
    "key2": "value2"
  }
}
```

## 測試

### 執行單元測試

```bash
mvn test
```

### 測試覆蓋率報告

```bash
mvn jacoco:report
```

測試報告位於：`target/site/jacoco/index.html`

## 監控

應用程式提供以下監控端點：

- `/actuator/health` - 健康檢查
- `/actuator/info` - 應用程式資訊
- `/actuator/metrics` - 效能指標
- `/test/redis/health` - Redis 專用健康檢查

## 常見問題

### Q: 如何更換 Redis 伺服器？
A: 修改 `application.yml` 中的 `framework.redis.standalone.host` 和 `port` 設定。

### Q: 如何使用 Redis Cluster？
A: 修改 `framework.redis.mode` 為 `cluster` 並配置 cluster 節點。

### Q: 資料沒有過期？
A: 檢查 TTL 設定和 Redis 伺服器時間。

### Q: 版本衝突錯誤？
A: 這是樂觀鎖機制，重新讀取資料並使用正確的版本號碼。

## 擴展功能

可以基於此範例擴展的功能：

1. **批次操作**: 支援大量資料的批次處理
2. **複雜查詢**: 實現更複雜的資料查詢邏輯
3. **事件通知**: 整合消息佇列進行資料變更通知
4. **快取策略**: 實現多層快取機制
5. **效能監控**: 添加詳細的效能指標

## 開發說明

此專案使用微服務框架的 @DL 模式，所有業務邏輯都封裝在獨立的組件中：

- 每個 @DL 組件都是獨立可測試的
- 支援複雜的輸入驗證和錯誤處理
- 提供完整的操作中繼資料
- 遵循 Clean Architecture 原則

## 授權

本專案使用 MIT 授權條款。