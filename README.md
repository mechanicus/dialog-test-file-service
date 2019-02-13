## File-service

Файловый сервис предназначен для загрузки файлов в хранилище клиентом мессенджера и
для скачивания ранее загруженных файлов из хранилища.
Когда клиент загружает файл на сервер, то на сервере для него генерирунтся уникальный
идентификатор в виде SHA-256 хэша, который и становится именем этого файла в каталоге
хранилища. Сгенерированный идентификатор файла вместе с дополнительной метаинформацией
возвращается клиенту.
В дальнейшем любой авторизованный клиент может скачать этот файл по этому хэшу.


## REST-API:

Загружаем файл на сервер:

запрос
```
curl \
    -X POST \
    -H 'Cookie: sessionId=dfc36da233fbc52cf81de5e8634c75de50e6f2bb75d283598a05b92f7acc29b4' \
    -H 'Content-Type: multipart/form-data' \
    -F "file=@document.pdf" \
    'http://localhost:8003/files/upload'
```
ответ
```
{
    "result" : {
        "fileId" : "c79de1d840f9f0c7e96c8097e424666fc1b473fc827587332121e3775cb2bb2f",
        "ownerId" : "533faa10d0f4645527c3193db27a683f11c202f72c8366210bca6bd001167e34",
        "originalName" : "document.pdf",
        "uploadedOn" : "2019-02-14T04:38:53.915"
    },
    "status" : "OK",
    "code" : 200
}
```

Скачиваем файл с сервера:

запрос
```
curl \
    -X GET \
    -H 'Cookie: sessionId=dfc36da233fbc52cf81de5e8634c75de50e6f2bb75d283598a05b92f7acc29b4' \
    'http://localhost:8003/files/download/c79de1d840f9f0c7e96c8097e424666fc1b473fc827587332121e3775cb2bb2f' \
    > document.pdf
```
