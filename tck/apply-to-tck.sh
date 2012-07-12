# This is a convenience script for copying the Repository RI to the OSGi TCK
TARGET_LOC=... the location of the OSGi repository containing the TCK ...
VERSION=... The version of the repostory RI ...
TARGET_VERSION=2.0.0

cp ../bundle/target/jbosgi-repository-$VERSION.jar $TARGET_LOC/licensed/repo/org.jboss.repository.jbosgi-repository/org.jboss.repository.jbosgi-repository-$TARGET_VERSION.jar
cp target/jbosgi-repository-tck-$VERSION.jar $TARGET_LOC/licensed/repo/org.jboss.repository.jbosgi-repository-tck/org.jboss.repository.jbosgi-repository-tck-$TARGET_VERSION.jar
