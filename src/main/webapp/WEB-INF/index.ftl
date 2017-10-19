<#include "/WEB-INF/base.ftl">

<#macro content>
    <form>
        <div class="input-group">
            <input type="text" name="q" class="form-control" placeholder="Search for..." value="${attr('q')!''}">
            <span class="input-group-btn">
                <button class="btn btn-default" type="submit" rel="no-follow">
                    <span class="glyphicon glyphicon-search"></span> Go!
                </button>
            </span>
        </div>
    </form>
    <#if attr('result')??>
        <#assign result=attr('result')/>
        <#if result.version??>
            <#if result.list?has_content>
                <#include "/WEB-INF/item.ftl">
                <@item item=result[0]/>
            <#else>
                <p class="text-info lead">No result for ${result.groupId!''}:${result.artifactId!''}:${result.version!''}</p>
            </#if>
        <#else>
            <ol class="breadcrumb">
                <#if result.artifactId??>
                    <li><a href="?groupId=${encode(result.groupId!'')}">${result.groupId!''}</a></li>
                    <li class="active">${result.artifactId!''}</li>
                <#else>
                    <li class="active">${result.groupId!''}</li>
                </#if>
            </ol>
            <#if result.count gt 0>
                <table class="table">
                    <tr>
                        <th>groupId</th>
                        <th>artifactId</th>
                        <th>version</th>
                    </tr>
                    <#list result.list as r>
                        <tr>
                            <td><a href="?groupId=${encode(r.groupId)}">${r.groupId}</a></td>
                            <td><a href="?groupId=${encode(r.groupId)}&artifactId=${encode(r.artifactId)}">${r.artifactId}</a></td>
                            <td><a href="?groupId=${encode(r.groupId)}&artifactId=${encode(r.artifactId)}&version=${encode(r.version)}">${r.version}</a></td>
                        </tr>
                    </#list>
                </table>
                <#if result.count gt 20>
                    <nav aria-label="Page navigation">
                        <ul class="pagination">
                            <#if result.offset == 0>
                                <li class="disabled"><a href='javascript:void(0)'><span aria-hidden="true">&laquo;</span></a></li>
                            <#else>
                                <li><a href="${result.base}${max(result.offset - 20, 0)}"><span aria-hidden="true">&laquo;</span></a></li>
                            </#if>                                
                            <#list result.pages as i>
                            		<#if i == -1>
                            			<li class="disabled"><a href='javascript:void(0)'>...</a></li>
                            		<#else>
	                                    <#assign page=i / 20 + 1 />
	                                    <#if i == result.offset>
	                                        <li class="disabled"><a href='javascript:void(0)'>${page?c}</a></li>
	                                    <#else>
	                                        <li><a href="${result.base}${i?c}">${page?c}</a></li>
	                                    </#if>
                                    </#if>
                                </#list>
                            <#if result.count - result.offset gt 20>
                                <li><a href="${result.base}${(result.offset + 20)?c}"><span aria-hidden=\"true\">&raquo;</span></a></li>
                            <#else>
                                <li class="disabled"><a href="javascript:void(0)"><span aria-hidden="true">&raquo;</span></a></li>
                            </#if>
                        </ul>
                    </nav>
                </#if>
            <#else>
                <p class="text-info lead">No result</p>
            </#if>
        </#if>
    </#if>
</#macro>
<@display_page/>