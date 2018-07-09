# API文档

本文档说明了搜索服务的调用方法

# 1 搜索

## 1.1 描述

传入一张图片、搜索的起止时间以及检索数量，返回符合要求的所有人脸
## 1.2 请求
调用地址：http://ip:28888/search
请求方式：post
请求类型：application/json

是否必选  | 参数名 | 类型 | 参数说明  
--------------|--------------|--------------|--------
是 |  base64 | String | 图片的base64编码
否 |  top    | Int | top k的值, 默认10
是 |  threshold | Double | 相似度阈值(0.0-100.0)，越大越相似，默认0.0
是 |  startTime | String | 搜索开始时间, 格式“yyyyMMdd”, 如"20180503"
是 |  endTime | String | 搜索结束时间, 格式“yyyyMMdd”,如"20180503"

## 1.3 返回
返回类型: JSON

参数名 | 类型 | 参数说明
---|---|---
code | Int | 返回状态码
time_used| Int | 整个请求花费的时间，单位毫秒
result | Array | top 10 个最相似的人脸, 包含人脸的相似度score 和 查询到的人脸的indexKey

## 1.4 返回示例

``` json
{
  "code": 101,
  "time_used":100,
  "result": [{"score":98.9,"indexKey":"/home/hadoop/hh.jpg"},{"score":97.6,"indexKey":"/home/hadoop/hh.jpg"}]
}
```
# 2 旧版接口

## 2.1 描述

传入一张图片、搜索的起止时间以及检索数量，返回符合要求的所有人脸
## 2.2 请求
调用地址：http://ip:28888/search
请求方式：post
请求类型：application/json

是否必选  | 参数名 | 类型 | 参数说明  
--------------|--------------|--------------|--------
是 |  base64 | String | 图片的base64编码
否 |  top    | Int | top k的值, 默认10
是 |  threshold | Double | 相似度阈值(0.0-100.0)，越大越相似，默认0.0
是 |  startTime | String | 搜索开始时间, 格式“yyyyMMdd”, 如"20180503"
是 |  endTime | String | 搜索结束时间, 格式“yyyyMMdd”,如"20180503"

## 2.3 返回
返回类型: JSON

参数名 | 类型 | 参数说明
---|---|---
code | Int | 返回状态码
time_used| Int | 整个请求花费的时间，单位毫秒
result | Array | top 10 个最相似的人脸, 包含人脸的相似度score 和 查询到的人脸的indexKey

## 2.4 返回示例

``` json
{
  "code": 101,
  "time_used":100,
  "result": [{"score":98.9,"indexKey":"/home/hadoop/hh.jpg"},{"score":97.6,"indexKey":"/home/hadoop/hh.jpg"}]
}
```

# 返回代码

code | 参数说明
---|---
101 | 正常
901 | 日期错误
902 | 请求图片未检测到人脸
903 | base64解析错误 

