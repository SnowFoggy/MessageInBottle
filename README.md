## 环境要求
- JDK 17
- Maven 3.9+
- MySQL 8.x

## 数据库
先在 MySQL 执行：
- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`

默认数据库配置在 `src/main/resources/application.yml`：
- 数据库名：`message_in_bottle`
- 端口：`8080`

如果你的本机 MySQL 账号密码不同，直接修改 `application.yml`。

## 启动
在 `MessageInBottle` 目录运行：

```bash
mvn spring-boot:run
```

## 已生成接口
- `POST /api/auth/register` 注册
- `POST /api/auth/login` 登录
- `GET /api/home/tasks` 首页任务列表
- `GET /api/home/categories` 首页分类
- `POST /api/tasks/publish` 发布任务
- `POST /api/tasks/{taskId}/accept` 接取任务
- `GET /api/wallet?userId=1` 钱包
- `GET /api/mine/published?userId=1` 我的发布
- `GET /api/mine/accepted?userId=1` 我的接取

## 返回格式
统一返回：

```json
{
  "success": true,
  "message": "获取成功",
  "data": {}
}
```

## Android 端
把本地模拟的 `AuthApiService`、首页任务源、我的钱包/发布/接取数据源，切换成 HTTP 请求即可。

