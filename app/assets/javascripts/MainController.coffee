window.MainController = ($scope, $http) ->
  $scope.branches = []
  
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

  $scope.restart_service = () ->
    $http.post("/restart/")
         .success((data, status, headers, config) -> location.reload())

  $scope.stop_service = () ->
    $http.post("/stop/")
    
  $scope.checkout_all = () ->
    $http.post("/checkoutAll/")
    
  $scope.checkout_commit = (hash) ->
    $http.post("/checkoutCommit/" + hash + "/")
         .success((data, status, headers, config) -> location.reload())
    
  $scope.checkout_branch = () ->
    $http.post("/checkoutBranch/" + $scope.currentSelectedBranch + "/")
         .success((data, status, headers, config) -> location.reload())    
  
  $scope.add_to_branches = (branch) ->
    $scope.branches.push(branch)
  
  $scope.clear_branches = () ->
    $scope.branches = []
  
  $scope.restart_cassandra = () ->
    $http.post("/cassandra/restart/")
  
  $scope.stop_cassandra = () ->
    $http.post("/cassandra/stop/")
  
  $scope.fetch_all = () ->
    $http.post("/fetchAll/")
         .success((data, status, headers, config) -> location.reload())
  
  $scope.pull_current = () ->
    $http.post("/pullCurrent/")
         .success((data, status, headers, config) -> location.reload())
  
  run_status_checker($scope, $http)
  check_status($scope, $http)
