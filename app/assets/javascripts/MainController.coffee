window.MainController = ($scope, $http) ->
  check_status = ($scope, $http) ->
    $http.get("/status/")
         .success((data, status, headers, config) ->
           $scope.status = (data == "Ok"))
         .error((data, status, headers, config) ->
           $scope.status = false)

  run_status_checker = ($scope, $http) ->
    setInterval(() ->
      check_status($scope, $http)
    ,
      2000)

  restart_service = ($http) ->
    $http.post("/restart/")
    location.reload()

  stop_service = ($http) ->
    $http.post("/stop/")
    
  checkout_commit = ($http, hash) ->
    $http.post("/checkoutCommit/" + hash + "/")

  run_status_checker($scope, $http)
  check_status($scope, $http)
  window.restart_service = () -> restart_service($http)
  window.stop_service = () -> stop_service($http)
  window.checkout_commit = (hash) -> checkout_commit($http)
