<#macro content></#macro>

<#macro messages>
    <#if attr('error')??>
        <div class="alert alert-danger alert-dismissible" role="alert">
            <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
            ${attr('error')}
        </div>
    </#if>
    <#if attr('success')??>
        <div class="alert alert-success alert-dismissible" role="alert">
            <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
            ${attr('success')}
        </div>
    </#if>
</#macro>

<#macro display_page>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Simple Repo</title>
<link href="${path}/css/${configuration.theme}.css" rel="stylesheet"/>
<script type='text/javascript' src="${path}/js/jquery-3.1.0.min.js"></script>
<script type='text/javascript' src="${path}/js/bootstrap.min.js"></script>
</head>
<body>
    <nav class="navbar navbar-inverse">
        <div class="container">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="/simple-repo/">Simple repo</a>
                </div><div id="navbar" class="collapse navbar-collapse">
                    <ul class="nav navbar-nav">
                        <li><a href="${path}/repository/">All repositories</a></li>
                        <li><a href="${path}/config">Configuration</a></li>
                        <li><a href="${path}/config/export">Export</a></li>
                    </ul>
                </div>
            </div>
        </nav>
        <div  class="container">
            <@content/>
        </div>
    </body>
</html>
</#macro>