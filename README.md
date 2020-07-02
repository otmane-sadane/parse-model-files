# parse-model-files
<b>The problem :</b></br>
When Hibernate generate database script, he creates columns in an alphabetical order wich is not necessarly the same order of attributes in model classes, if you use heritage, he will use heritage order first and then alphabetical order.</br>
Exemple :</br>
-in java parent class AbstractEntity you have attributes :</br>
       1 - Long id</br>
       2 - Long version</br>
       3 - Boolean deletionFlag</br>
  -in java class "student" you have attributes :</br>
       1 - String name</br>
       2 - Integer age</br>
  
  -Hibernate will generate sql script like :</br>
            create table student ( [ id bigint,  deletion_flag boolean,  version bigint], [age int, name varchar2(30)]) ;</br>
<b>So what !? </b></br>
So Im not happy with this sql column order : create table student ( [ id bigint,  deletion_flag boolean,  version bigint], [age int, name varchar2(30)]) ;</br>
I want sql order to respect the following : </br>
1 - put attributes of coming from my business model class (Student) first, and not the herited attributes from (AbstractEntity) </br>
2 - put attributes inthe sql qscript in same order as the business model class </br>
to be same as Java order,  I mean : create table student ( name varchar2(30),  age int ) ;</br>

<b> To be completed ... </b>
