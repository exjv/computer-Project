# 接口说明
统一返回：`{ code, message, data }`，分页参数：`current,size`。

## AuthController
- POST `/api/auth/login`
- GET `/api/auth/userInfo`
- PUT `/api/auth/updatePassword`
- PUT `/api/auth/updateProfile`
- POST `/api/auth/logout`

## UserController
- GET `/api/users/page`
- POST `/api/users`
- PUT `/api/users/{id}`
- DELETE `/api/users/{id}`
- PUT `/api/users/{id}/reset-password`
- PUT `/api/users/{id}/status`
- GET `/api/users/list-by-role`

## DeviceController
- GET `/api/devices/page`
- GET `/api/devices/{id}`
- POST `/api/devices`
- PUT `/api/devices/{id}`
- DELETE `/api/devices/{id}`
- GET `/api/devices/statistics`

## RepairOrderController
- GET `/api/repair-orders/page`
- GET `/api/repair-orders/my`
- GET `/api/repair-orders/{id}`
- POST `/api/repair-orders`
- PUT `/api/repair-orders/{id}`
- PUT `/api/repair-orders/{id}/assign`
- PUT `/api/repair-orders/{id}/status`
- GET `/api/repair-orders/statistics`

## RepairRecordController
- GET `/api/repair-records/page`
- GET `/api/repair-records/{id}`
- POST `/api/repair-records`
- PUT `/api/repair-records/{id}`
- DELETE `/api/repair-records/{id}`

## NoticeController
- GET `/api/notices/page`
- GET `/api/notices/{id}`
- POST `/api/notices`
- PUT `/api/notices/{id}`
- DELETE `/api/notices/{id}`

## LogController
- GET `/api/logs/operation/page`
- GET `/api/logs/login/page`
