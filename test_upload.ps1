$filePath = "D:\daima\Furniture\frontend\src\assets\styles\main.css"
$uri = "http://localhost:9090/api/files/upload"

$webClient = New-Object System.Net.WebClient
$response = $webClient.UploadFile($uri, $filePath)
$webClient.Dispose()

$content = [System.Text.Encoding]::UTF8.GetString($response)
Write-Host "响应内容: $content"
