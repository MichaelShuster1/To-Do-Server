package server.model;

import java.util.Objects;

public class Todo
{
    private static int idGenerator=1;
    private Integer id;
    private String title;
    private String content;
    private String status;
    private Long dueDate;



    public Todo(String title, String content,Long dueDate)
    {
        this.title = title;
        this.content = content;
        this.dueDate = dueDate;
        this.id=idGenerator++;
        this.status="PENDING";
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getId()
    {
        return id;
    }
    public String getTitle()
    {

        return title;
    }

    public Long getDueDate()
    {
        return dueDate;
    }

    public String getStatus()
    {
        return status;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString()
    {
        return "Todo{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", due_date=" + dueDate +
                ", status='" + status + '\'' +
                '}';
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Todo todo = (Todo) o;
        return id == todo.id && Objects.equals(title, todo.title) && Objects.equals(content, todo.content) && Objects.equals(dueDate, todo.dueDate) && Objects.equals(status, todo.status);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, title, content, dueDate, status);
    }




}
