# frozen_string_literal: true

begin
  require 'github_changelog_generator/task'
rescue LoadError
  task :changelog do
    abort('Run `bundle install --with release` to install the `github_changelog_generator` gem.')
  end
else
  GitHubChangelogGenerator::RakeTask.new :changelog do |config|
    config.header = <<~HEADER.chomp
      # Changelog

      All notable changes to this project will be documented in this file.
    HEADER
    config.user = 'OpenVoxProject'
    config.project = 'trapperkeeper-webserver'
    config.exclude_labels = %w[dependencies duplicate question invalid wontfix wont-fix modulesync skip-changelog]
    config.future_release = File.readlines('project.clj')
      .grep(/^\(defproject /).first[/"([^"]+)"/, 1]
      .sub(/-SNAPSHOT\z/, '')
    # The generated changelog starts with 1.0.19, the first release after the move to OpenVoxProject.
    config.since_tag = '1.0.18'
    config.release_branch = 'main'
  end
end

namespace :vox do
  desc 'Set the version in project.clj for the release'
  task 'version:bump:full', [:version] do |_, args|
    version = args[:version]
    abort 'You must provide a version.' if version.nil? || version.empty?

    project = File.read('project.clj')
    defproject = /^(\(defproject\s+\S+\s+)"[^"]+"/
    abort 'Could not find the defproject version in project.clj' unless project.match?(defproject)

    File.write('project.clj', project.sub(defproject) { "#{Regexp.last_match(1)}\"#{version}\"" })
    puts "Set the version in project.clj to #{version}"
  end
end

desc 'Prepare for a release'
task 'release:prepare' => [:changelog]
