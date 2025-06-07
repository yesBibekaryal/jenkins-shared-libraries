#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'sananddev'
    def gitUserEmail = config.gitUserEmail ?: 'kinarapari@gmail.com'
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Configure Git
        sh """
            git config user.name sananddev
            git config user.email kinarapari@gmail.com
        """
        
        // Update deployment manifests with new image tags - using proper Linux sed syntax
        sh """
            # Update main application deployment - note the correct image name is trainwithshubham/easyshop-app
            sed -i "s|image: sandyswosti/easyshop-app:.*|image: sandyswosti/easyshop-app:${imageTag}|g" https://github.com/sananddev/tws-e-commerce-app/blob/master/kubernetes/08-easyshop-deployment.yaml
            
            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: sandyswosti/easyshop-migration:.*|image: sandyswosti/easyshop-migration:${imageTag}|g" https://github.com/sananddev/tws-e-commerce-app/blob/master/kubernetes/12-migration-job.yaml
            fi
            
            # Ensure ingress is using the correct domain
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" https://github.com/sananddev/tws-e-commerce-app/blob/master/kubernetes/10-ingress.yaml
            fi
            
            # Check for changes
            if git diff --quiet; then
                echo "No changes to commit"
            else
                # Commit and push changes
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                
                # Set up credentials for push
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/sananddev/tws-e-commerce-app.git
                git push origin HEAD:\${GIT_BRANCH}
            fi
        """
    }
}
