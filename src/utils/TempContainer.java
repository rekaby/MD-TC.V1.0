package utils;

import java.util.List;

public class TempContainer<Integer> {
    private List<Integer> items; 
    public void setItems(List<Integer> items) {
       this.items = items;
    }

    public List<Integer> getItems() {
         return items;
    }
}