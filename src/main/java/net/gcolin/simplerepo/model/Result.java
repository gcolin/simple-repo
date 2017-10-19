/*
 * Copyright 2017 gcolin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.gcolin.simplerepo.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gcolin
 */
public class Result {

    private final List<SearchResult> list = new ArrayList<>();
    private int count;
    private int offset;
    private String groupId;
    private String artifactId;
    private String version;
    private String base;
        
    public List<SearchResult> getList() {
        return list;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
    
    public List<Integer> getPages() {
        List<Integer> list = new ArrayList<>();
        list.add(0);
        int p = (offset / 20) * 20;
        int m = (count / 20 + (count % 20 == 0 ? -1 : 0)) * 20;
        int pp = p - 20;
        if(pp > 0) {
          if(pp > 20) {
            list.add(-1);
          }
          list.add(pp);
        }
        list.add(p);
        pp = p + 20;
        if(pp < m) {
          list.add(pp);
          if(pp < m - 20) {
            list.add(-1);
          }
        }
        if(m != p) {
          list.add(m);
        }
        return list;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }
    
    

}
