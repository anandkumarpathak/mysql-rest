

angular.module('$moduleName', []).factory('$serviceName', function($http){
	$http.defaults.headers.get = {'Cache-Control':'no-cache'};
	return {#foreach($method in $methods)		
		
		$method.name: function(#foreach( $param in $pathParams[$method])$param#if($foreach.hasNext), #end#end#if($queryParams[$method])#foreach($param in $queryParams[$method])$param#if($foreach.hasNext), #end#end#end) {
		           return $http({
		                	method: '$httpMethods[$method]',
		                	url: $methodPaths[$method],
		                	params: {#foreach($param in $queryParams[$method])'$param': $param#if($foreach.hasNext), #end#end}
			        	});
			}#if($foreach.hasNext),#end
#end
	   
	}
});