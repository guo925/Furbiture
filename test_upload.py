import requests

# 测试文件上传接口
file_path = r'D:\daima\Furniture\frontend\src\assets\styles\main.css'
url = 'http://localhost:9090/api/files/upload'

with open(file_path, 'rb') as f:
    files = {'file': f}
    response = requests.post(url, files=files)
    
print('响应状态码:', response.status_code)
print('响应内容:', response.json())
