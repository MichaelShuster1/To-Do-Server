package server.controller;


import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.model.Todo;
import server.model.Utilities;

import java.util.*;

@RestController
public class Controller
{
    Logger requestLogger = LoggerFactory.getLogger("request-logger");


    int counter=1;
    private List<Todo> todoList=new ArrayList<>();

    @GetMapping("/todo/health")
    public String Health()
    {
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/health | HTTP Verb GET");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;
        return "OK";
    }

    @PostMapping(value = "/todo",consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String,Object>> CreateNewTODO(@RequestBody Map<String,String> requestBody)
    {
        Map<String,Object> bodyResponse=new HashMap<>();
        String title =requestBody.get("title");
        Long dueDate =Long.valueOf(requestBody.get("dueDate"));


        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo | HTTP Verb POST");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if(Utilities.checkIfExistByTitle(todoList,title))
        {
            bodyResponse.put("errorMessage","Error: TODO with the title "
                    + title+ " already exists in the system");
            return new ResponseEntity<>(bodyResponse, HttpStatus.CONFLICT);
        }
        if(dueDate.compareTo(new Date().getTime())<0)
        {
            bodyResponse.put("errorMessage","Error: Can’t create new TODO that its due date is in the past");
            return new ResponseEntity<>(bodyResponse, HttpStatus.CONFLICT);
        }
        Todo todo =new Todo(title,requestBody.get("content"),dueDate);
        todoList.add(todo);
        bodyResponse.put("result",todo.getId());
        return  new ResponseEntity<>(bodyResponse, HttpStatus.OK);
    }



    @GetMapping("/todo/size")
    public ResponseEntity<Map<String,Integer>> getTODOsCount(@RequestParam String status)
    {
        Map<String,Integer> bodyResponse=new HashMap<>();

        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/size | HTTP Verb GET");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if(status.equals("ALL"))
        {
            bodyResponse.put("result",todoList.size());
            return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
        }
        if(Utilities.STATUSES.contains(status))
        {
            return Utilities.countTODO(todoList,status);
        }
        return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
    }



    @GetMapping("/todo/content")
    public ResponseEntity<List<Todo>> getTODOsData(@RequestParam String status,@RequestParam(required = false) String sortBy)
    {
        List<Todo> bodyResponse=new ArrayList<>();


        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/content | HTTP Verb GET");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if( ( !Utilities.STATUSES.contains(status) && !status.equals("ALL") ) || ( sortBy!=null && !Utilities.SORTS_BY.contains(sortBy) ) )
        {
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }

        if(status.equals("ALL"))
            bodyResponse=todoList;
        else
            bodyResponse=Utilities.getTODO(todoList,status);

        if(sortBy==null||sortBy.equals("ID"))
            Utilities.SortById(bodyResponse);
        else
        {
            if (sortBy.equals("TITLE"))
                Utilities.SortByTitle(bodyResponse);
            if (sortBy.equals("DUE_DATE"))
                Utilities.SortByDate(bodyResponse);
        }

        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }

    @PutMapping("/todo")
    public ResponseEntity<Map<String,String>> UpdateTODOStatus(@RequestParam Integer id,@RequestParam String status)
    {
        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/ | HTTP Verb PUT");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if(!Utilities.STATUSES.contains(status))
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);

        Todo todo;
        String oldStatus;
        Map<String,String> bodyResponse=new HashMap<>();

        todo=Utilities.FindTODObyId(todoList,id);

        if(todo==null)
        {
            bodyResponse.put("errorMessage","Error: no such TODO with id "+id);
            return new ResponseEntity<>(bodyResponse,HttpStatus.NOT_FOUND);
        }

        oldStatus=todo.getStatus();
        todo.setStatus(status);
        bodyResponse.put("result",oldStatus);
        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }

    @DeleteMapping("/todo")
    public ResponseEntity<Map<String,Object>> DeleteTODO(@RequestParam Integer id)
    {
        Map<String,Object> bodyResponse=new HashMap<>();
        Todo todo =Utilities.FindTODObyId(todoList,id);

        requestLogger.info("Incoming request | #"+ counter +" | resource: /todo/ | HTTP Verb DELETE");
        requestLogger.debug("request #"+counter+" duration: "+new Date().getTime()+"ms");
        counter++;

        if(todo==null)
        {
            bodyResponse.put("errorMessage","Error: no such TODO with id "+id);
            return new ResponseEntity<>(bodyResponse,HttpStatus.NOT_FOUND);
        }
        todoList.remove(todo);
        bodyResponse.put("result",todoList.size());
        return new ResponseEntity<>(bodyResponse,HttpStatus.OK);
    }
}