<?php  
	$url = "";
	if(isset($_GET['url'])) {
		$url = $_GET['url'];
	} 
	if(isset($_GET['topic'])) {
		$url = $url.'&topic='.$_GET['topic'];
	} 
	if(isset($_GET['output'])) {
		$url = $url.'&output='.$_GET['output'];
	} 
	
	if(isset($_GET['v'])) {
		$url = $url.'&v='.$_GET['v'];
	} 
	
	if(isset($_GET['q'])) {
		$url = $url.'&q='.$_GET['q'];
	} 
	

	
	//else {
		//echo 'failed access url: '.$url;
	//}
	
	$content = file_get_contents($url);
	
	if(!$content) {

		echo 'failed obtain content at: '.$url;
	} else {
		header('Content-Length: '.strlen($content));
		header("Content-type: text/xml");
		echo $content;
	}

?>
