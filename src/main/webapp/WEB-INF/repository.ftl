<#include "/WEB-INF/base.ftl">

<#macro content>
    <#assign reponame=attr('r')!''/>
    <div class="row">
        <div class="col-sm-3">
            <div class="panel panel-info">
                <div class="panel-heading">
                    <span class="glyphicon glyphicon-hdd"></span> Repositories
                </div>
                <div class="panel-body">
                    <ul class="nav nav-pills nav-stacked">
                        <#list configurationManager.repositories as repo>
                            <li role="presentation" <#if reponame==repo.name>class='active'</#if>>
                                <a href='?r=${encode(repo.name)}'>${repo.name}</a>
                            </li>
                        </#list>
                    </ul>
                </div>
            </div>
        </div>
        <div class='col-sm-9'>
            <div>
                <a class='btn btn-default' href='?a=new'>
                    <span class='glyphicon glyphicon-plus'></span> New repository
                </a>
                <a class='btn btn-info' href='?a=global'>
                    <span class='glyphicon glyphicon-cog'></span> Global configuration
                </a>
            </div>
            <br/>
            <#if attr('a')?? && attr('a') == "global">
                <div class="panel panel-default">
                    <div class="panel-heading">
                        <span class="glyphicon glyphicon-cog"></span> Global configuration 
                        <a class="pull-right" href='?'>X</a>
                    </div>
                    <div class="panel-body">
                        <@messages/>
                        <form method="POST">
                            <input type="hidden" name="a" value='global'/>
                            <div class="form-group">
                                <label for="maxsnapshots">Max snapshots</label>
                                <input type="text" name="maxsnapshots" class="form-control" id="maxsnapshots" placeholder="The max number of snaphots by artifact" value="${(attr('maxsnapshots')!configurationManager.maxSnapshots)?c}"/>
                            </div>
                            <div class="form-group">
                                <label for="maxsnapshots">Not Found Cache</label>
                                <input type="text" name="notfoundcache" class="form-control" id="notfoundcache" placeholder="The time in millisenconds before retrying to get a not found file" value="${(attr('notfoundcache')!configurationManager.notFoundCache)?c}"/>
                            </div>
                            <div class="form-group">
                                <label for="theme">Theme</label>
                                <select id="theme" name="theme" class="form-control">
                                    <#list themes as theme>
                                        <option value='${theme}' <#if theme == configurationManager.configuration.theme>selected="selected"</#if>>${theme}</option>
                                    </#list>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="elasticsearch">Elasticsearch URL</label>
                                <input type="text" name="elasticsearch" class="form-control" id="elasticsearch" placeholder="" value="${(attr('elasticsearch')!configurationManager.elasticsearch)?c}"/>
                            </div>
                            <button type="submit" class="btn btn-primary">
                                <span class="glyphicon glyphicon-floppy-disk"></span> Save</button>
                        </form>
                    </div>
                </div>
            <#elseif attr('selected')??>
                <#assign selected=attr('selected')/>
                <#assign isNew=attr('isNew')/>
                <div class="panel panel-default">
                    <div class="panel-heading"><span class="glyphicon glyphicon-hdd"></span> 
                        <#if isNew>New<#else>Edit</#if> repository 
                        <a class="pull-right" href='?'>X</a>
                    </div>
                    <div class="panel-body">
                        <@messages/>
                        <form method="POST">
                            <div class="form-group">
                                <#if isNew>
                                    <input type="hidden" name="a" value='new'/>
                                <#else>
                                    <input type="hidden" name="a" value=''/>
                                    <input type="hidden" name="r" value='${selected.name!''}'/>
                                </#if>
                                <label for="name">Name</label>
                                <input type="text" name="name" class="form-control" id="name" placeholder="Name" value="${attr('name')!selected.name!''}"/>
                            </div>
                            <div class="form-group">
                                <label for="remote">Remote URL</label>
                                <input type="text" name="remote" class="form-control" id="remote" placeholder="URL" value="${attr('remote')!selected.remote!''}"/>
                            </div>
                            <div class="form-group">
                                <label for="included">Includes</label>
                                <select id="included" name="included" multiple="" class="form-control">
                                    <#list configurationManager.repositories as repo>
                                        <#if !selected.name?? || repo.name != selected.name>
                                            <option value='${repo.name}' <#if selected.includes?? && selected.includes?seq_contains(repo.name)>selected="selected"</#if>>${repo.name}</option>
                                        </#if>
                                    </#list>
                                </select>
                            </div>
                            <button type="submit" class="btn btn-primary">
                                <span class='glyphicon glyphicon-floppy-disk'></span>
                                <#if isNew>Create<#else>Update</#if>
                            </button>
                            <#if !isNew>
                                <button type="button" onclick="javascript:$(this).closest('form').find('input[name=a]').val('del').closest('form').submit()" class="btn btn-warning">
                                    <span class='glyphicon glyphicon-trash'></span> Remove
                                </button>
                            </#if>
                        </form>
                    </div>
                </div>
            </#if>
        </div>
    </div>
</#macro>
<@display_page/>