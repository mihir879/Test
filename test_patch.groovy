import java.text.SimpleDateFormat  
import javax.net.ssl.HttpsURLConnection
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import groovy.time.*


//Build Job Method
@NonCPS
def buildJob(String jobName, int quietTime, String[] parameters){
       println "building job $jobName"
        List<ParameterValue> params = new ArrayList<ParameterValue>()
        for(String param : parameters){
        params.add(new StringParameterValue(param.split('=')[0], param.split('=')[1]))
       }  
    def job = Hudson.instance.getJob(jobName)
    def next_build = job.nextBuildNumber
    def paramsAction = new ParametersAction(params) 
    Jenkins.instance.queue.schedule(job, quietTime, null, paramsAction)
    def buildnumber = Jenkins.instance.getItemByFullName(jobName)
    def build = buildnumber.getLastBuild().number
    println ""+env.JENKINS_URL+""+job.url+""+next_build
    println "next build number is "+next_build
    wait_for_build_complete(jobName, next_build)
}

//Build WAIT Method
@NonCPS
def wait_for_build_complete(String jobName, int build_number) {
      //   def job_wait = Jenkins.instance.getItemByFullName(jobName)
       //  def build_wait = job_wait.getBuildByNumber(build_number)
      //  def build_number_wait = build_wait.getNumber() 
       // def isBuilding = build_wait.isBuilding()
       // def inProgress = build_wait.isInProgress()
       // while  ( !(inProgress == false && isBuilding == false) )  {
        //     println "This build is running"
         //    try {
            //           Thread.sleep(5000)
             //          build_wait = job_wait.getBuildByNumber(build_number_wait)
               //        isBuilding = build_wait.isBuilding()
              //         inProgress = build_wait.isInProgress()
	   //   } catch(e) {
                    //    println e
			//println "\033[31m[Error] >>>>>>>>>>>>> User Aborted <<<<<<<<<<<<<<<< \033[0m"
			//return null
	   //   }
        //}
    //	return null
	 def job_wait = Jenkins.instance.getItemByFullName(jobName)
        def build_wait = job_wait.getBuildByNumber(build_number)
 
        while(build_wait.isInProgress()) {
             println "This build is running"
             try {
                       Thread.sleep(5000)
                      
            } catch(e) {
                        if(e instanceof org.jenkinsci.plugins.workflow.steps.FlowInterruptedException)
                             println "\033[31m[Error] >>>>>>>>>>>>> User Aborted <<<<<<<<<<<<<<<< \033[0m"
                                    else
                                        println e
            }
        }
}

//CSV file read method
@NonCPS
def csvReader(String FILENAME2)
{
	final CSV_HEADER_SERVERS = "Servers"
	def inputFile = new File(Filename)
	def serversindex = null
	def readHeader = false
	def servers=null
	
	inputFile.splitEachLine(",") { fields ->
		
		if(readHeader == false) {
			
   def len = fields.size()

   for(int i=0; i<len; i++) {
	   
	   if (fields[i].equalsIgnoreCase(CSV_HEADER_SERVERS))
	   serversindex = i
	       }

  }
		
		if(readHeader) {
			servers=fields[serversindex]
			println "servers: $servers"
			
			try{ 
			  buildJob('K03_OPC_KSPLICE_Install_N_Patch', 0, (String[])["REMOTEHOST=$servers", "REMOTEUSER=opc", "REMOTEKEY=/home/opc/.ssh/forsan"])
					//build job: 'K03_OPC_KSPLICE_Install_N_Patch', 
					//parameters: [
					//string(name: 'REMOTEHOST', value: ip),
					//string(name: 'REMOTEUSER', value: 'opc'),
					//string(name: 'REMOTEKEY', value: '/home/opc/.ssh/forpatch')
					//]
				}
				catch(err) {
					println "Error patching $servers but will move to next machine."
				}
	println "Done ksplice patching"
			    println "Done ksplice, start yum update"
				try{ 
				buildJob('K03.1_OS_YUM_UPDATES', 0, (String[])["REMOTEHOST=$servers", "REMOTEUSER=opc", "REMOTEKEY=/home/opc/.ssh/forsan"])
				//build job: 'K03.1_OS_YUM_UPDATES', 
					//parameters: [
					//string(name: 'REMOTEHOST', value: ip),
					//string(name: 'REMOTEUSER', value: 'opc'),
					//string(name: 'REMOTEKEY', value: '/home/opc/.ssh/forpatch')
					//]
					}
				catch(err) {
					println "Error with yum $ip but will move to next machine."
				}
			  
				println "Done YUM patching"
				
				println "Done YUM Update, checking qualys agent"
				try{ 
				buildJob('K02_Install_Qualys', 0, (String[])["REMOTEHOST=$servers", "REMOTEUSER=opc", "REMOTEKEY=/home/opc/.ssh/forsan"])
				// build job: 'K02_Install_Qualys', 
					//parameters: [string(name: 'REMOTEHOST', value: ip), 
					//string(name: 'REMOTEUSER', value: 'opc'), 
					//string(name: 'REMOTEKEY', value: '/home/opc/.ssh/forpatch')
					//]
					}
				catch(err) {
					println "Error with qualys $ip but will move to next machine."
				}
			  
				println "Done Qualys checking"		
			
			} // if IP matches string
			else { // not exact 
				println "SKIPING:  Host with ip $ip doesn't match $SubnetFilter"
				
			}
    
   }
  readHeader = true
 }
}

return this
