package server.model;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Utilities
{
    public static final List<String> STATUSES = List.of("PENDING", "LATE", "DONE");
    public static final List<String> SORTS_BY = List.of("ID", "TITLE", "DUE_DATE");

    public static boolean checkIfExistByTitle(List<Todo> todoList,String title)
    {
        for(Todo todo:todoList)
        {
            if(todo.getTitle().equals(title))
                return true;
        }
        return false;
    }

    public static ResponseEntity<Map<String,Integer>> countTODO(List<Todo> todoList,String status)
    {
        Map<String,Integer> bodyResponse=new HashMap<>();
        int count=0;
        for(Todo todo:todoList)
        {
            if(todo.getStatus().equals(status))
                count++;
        }
        bodyResponse.put("result",count);
        return new ResponseEntity<>(bodyResponse, HttpStatus.OK);
    }

    public static List<Todo> getTODO(List<Todo> todoList,String status)
    {
        List<Todo> bodyResponse =new ArrayList<>();
        for(Todo todo:todoList)
        {
            if(todo.getStatus().equals(status))
            {
                bodyResponse.add(todo);
            }
        }
        return bodyResponse;
    }

    public static void SortById(List<Todo> todoList)
    {
        Collections.sort(todoList, new Comparator<Todo>() {
            @Override
            public int compare(Todo o1, Todo o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }

    public static void SortByTitle(List<Todo> todoList)
    {
        Collections.sort(todoList, new Comparator<Todo>() {
            @Override
            public int compare(Todo o1, Todo o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
    }

    public static void SortByDate(List<Todo> todoList)
    {
        Collections.sort(todoList, new Comparator<Todo>() {
            @Override
            public int compare(Todo o1, Todo o2) {
                return o1.getDueDate().compareTo(o2.getDueDate());
            }
        });
    }


    public static Todo FindTODObyId(List<Todo> todoList, Integer id)
    {
        for(Todo todo:todoList)
        {
            if(todo.getId().equals(id))
            {
                return todo;
            }
        }
        return null;
    }
}
