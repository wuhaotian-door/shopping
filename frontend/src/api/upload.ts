import request from './request'
import type { Result } from '@/types/api'

/** 通用文件上传（自动区分图片/模型） */
export function uploadFile(file: File, type: 'image' | 'model' = 'image'): Promise<Result<string>> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return request.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}