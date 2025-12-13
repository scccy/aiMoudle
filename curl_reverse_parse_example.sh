#!/bin/bash

# 反向解析接口测试 curl 命令示例
# POST /api/reverse-parse/generate

curl -X POST http://localhost:40000/api/reverse-parse/generate \
  -H "Content-Type: application/json" \
  -d '{
  "modelName": "doubao-seedance-1-0-pro-250528",
  "requestBody": {
    "model": "doubao-seedance-1-0-pro-250528",
    "temperature": 0.8,
    "content": [
      {
        "type": "text",
        "text": "多个镜头。一名侦探进入一间光线昏暗的房间。他检查桌上的线索，手里拿起桌上的某个物品。镜头转向他正在思索。 --ratio 16:9"
      }
    ]
  },
  "requestHeaders": {
    "Content-Type": "application/json",
    "Authorization": "Bearer $ARK_API_KEY"
  },
  "paramMapping": [
    {
      "key": "model",
      "postParam": "model"
    },
    {
      "key": "temperature",
      "postParam": "temperature",
      "validate": "{\"range\":[0.0,2.0]}"
    },
    {
      "key": "content_type",
      "postParam": "type",
      "validate": "{\"enum\":[\"text\",\"image_url\"]}"
    },
    {
      "key": "ratio",
      "postParam": "text"
    },
    {
      "key": "prompt",
      "postParam": "text",
      "validate": "{\"maxLength\":2000}"
    }
  ],
  "headerMapping": [
    {
      "key": "contentTypeHeader",
      "postParam": "Content-Type"
    },
    {
      "key": "authorization",
      "postParam": "Authorization"
    }
  ]
}'

