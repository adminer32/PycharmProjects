import request from '@/utils/request'

export const uploadFile = async (file: File): Promise<string> => {
  const formData = new FormData()
  formData.append('file', file)
  
  const res = await request.post<any>('/api/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return res as unknown as string
}
