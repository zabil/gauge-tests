Spec Tagged Expression Execution
================================

* In an empty directory initialize a project named "spec_tagged_expression_exec" with the current language
* Add tags "tag1" to specification "tags_expression_execution"
* Create a scenario "second scenario" in specification "tags_expression_execution" with the following steps with implementation 

     |step text           |implementation               |
     |--------------------|-----------------------------|
     |Second Scenario step|"inside second scenario step"|

* Add tags "tag1, tag2" to scenario "second scenario" in specification "tags_expression_execution"
* Create a scenario "third scenario" in specification "tags_expression_execution" with the following steps with implementation 

     |step text          |implementation              |
     |-------------------|----------------------------|
     |Third Scenario step|"inside third scenario step"|

* Add tags "tag3, tag4" to scenario "third scenario" in specification "tags_expression_execution"
* Create a scenario "fourth scenario" in specification "tags_expression_execution" with the following steps with implementation 

     |step text           |implementation               |
     |--------------------|-----------------------------|
     |fourth Scenario step|"inside fourth scenario step"|

* Add tags "tag3, tag2" to scenario "fourth scenario" in specification "tags_expression_execution"

tagged expression
-----------------

* Execute the tags "!tag1" in spec "tags_expression_execution" and ensure success
* Console should not contain following lines 

     |console output             |
     |---------------------------|
     |inside second scenario step|
     |inside third scenario step |
     |inside fourth scenario step|
     
* Execute the tags "tag1 && tag2" in spec "tags_expression_execution" and ensure success
* Console should contain following lines in order 

     |console output             |
     |---------------------------|
     |inside second scenario step|
     |inside fourth scenario step|
     
* Console should not contain following lines 

     |console output            |
     |--------------------------|
     |inside third scenario step|
     
* Execute the tags "tag1 && (tag2|tag4)" in spec "tags_expression_execution" and ensure success
* Console should contain following lines in order 

     |console output             |
     |---------------------------|
     |inside second scenario step|
     |inside third scenario step |
     |inside fourth scenario step|
