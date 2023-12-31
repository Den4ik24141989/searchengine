# searchengine

Данный проект представляет из себя поисковой движок, обладающий следующими функциями:
- предварительное индексирование сайтов;
- выдача основных сведений о сайтах;
- поиск по ключевым словам и предоставление  страниц, прошедшим индексацию.

# Используемые технологии
- Java 17
- Framework
	- Spring boot
		- spring-boot-starter-web
		- spring-boot-starter-thymeleaf
		- spring-boot-starter-data-jpa
- Библиотеки
	- JSOUP
	- MySQL
	- Lombok 
	- LuceneMorphology
		- russian
- Многопоточность

# Файлы конфигурации и настроек

## application.yaml

- ### Server
Здесь задается порт для запуска приложения.
- ### Spring
В этом разделе заполняется информация для работы с БД, логин пароль и URL.
- ### Indexing-settings 
Здесь задаются сайты, которые будут проходить индексацию.

# Инструкция по запуску

- создать БД,
- ознакомиться с файлом application.yaml, установить соответствие логина, пароля, url БД  в разделе  "spring".
- запустить приложение
- после того как приложение запущено, в адресной строке браузера ввести localhost:8081.

Мы переходим на веб-страницу с тремя вкладками, первая из них:
### DASHBOARD 
![Screenshot 1](https://github.com/Den4ik24141989/searchengine/assets/132040633/624fdb92-0566-40a7-b0f7-4cfe9950c3d3)

На ней отображается общая статистика по всем проиндексированным сайтам, а также детальная статистика и статус по каждому из сайтов.

Следующая вкладка:
### MANAGEMENT
![Screenshot 2](https://github.com/Den4ik24141989/searchengine/assets/132040633/a80017a9-17b5-4451-bc43-7be9dba93697)

На этой вкладке находятся инструменты управления: запуск и остановка полной индексации, а также возможность добавить или обновить отдельную страницу.

Вкладка:
### SEARCH
![Screenshot 3](https://github.com/Den4ik24141989/searchengine/assets/132040633/b459ea0d-3c29-4145-8e70-0adde4dbd5cc)

На ней находится поле поиска и выпадающий список сайтов, по которому выполняется поиск, а при нажатии на кнопку SEARCH выводятся результаты поиска.

Поиск можно совершать как по всем сайтам: 
![Screenshot 4](https://github.com/Den4ik24141989/searchengine/assets/132040633/f527b73a-c95c-4e39-bfc4-8e5bb157100e)

Так и по отдельным в частности: 
![Screenshot 5](https://github.com/Den4ik24141989/searchengine/assets/132040633/32f807ec-5e81-452d-9f5a-90e0af4dad83)


