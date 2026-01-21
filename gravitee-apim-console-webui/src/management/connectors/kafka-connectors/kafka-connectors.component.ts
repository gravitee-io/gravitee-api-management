/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ClusterService } from '../../../services-ngx/cluster.service';
import { AddClusterDialogComponent, ClusterConfig } from '../add-cluster-dialog/add-cluster-dialog.component';
import { AddTopicDialogComponent, TopicConfig } from '../add-topic-dialog/add-topic-dialog.component';
import { KafkaManagementService } from '../services/kafka-management.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

interface ConnectorData {
  name: string;
  type: 'Source' | 'Sink';
  status: 'Running' | 'Paused' | 'Failed';
  cluster: string;
  tasks: string;
  lastUpdate: string;
  class: string;
  topics?: string[];
}

interface TopicData {
  id: string;
  name: string;
  partitions: number;
  replicas: number;
  messages: string;
  createdAt?: Date;
}

interface BrokerData {
  id: number;
  host: string;
  port: number;
  status: 'online' | 'offline';
}

@Component({
  selector: 'kafka-connectors',
  templateUrl: './kafka-connectors.component.html',
  styleUrls: ['./kafka-connectors.component.scss'],
  standalone: false,
})
export class KafkaConnectorsComponent implements OnInit {
  clusters: any[] = [];
  connectors: ConnectorData[] = [];
  topics: TopicData[] = [];
  brokers: BrokerData[] = [];
  selectedCluster: any = null;
  selectedTab: number = 0;
  isLoading = true;
  searchTerm: string = '';
  topicSearchTerm: string = '';
  displayedColumns: string[] = ['name', 'type', 'status', 'class', 'tasks', 'actions'];
  displayedTopicColumns: string[] = ['name', 'partitions', 'replicas', 'messages', 'actions'];
  customClusters: any[] = [];

  constructor(
    private clusterService: ClusterService,
    private dialog: MatDialog,
    private kafkaManagementService: KafkaManagementService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.loadClusters();
    this.loadCustomClusters();
  }

  loadCustomClusters() {
    // Load custom cluster configs from localStorage
    const stored = localStorage.getItem('kafka_custom_clusters');
    if (stored) {
      try {
        this.customClusters = JSON.parse(stored);
      } catch (e) {
        console.error('Error loading custom clusters', e);
        this.customClusters = [];
      }
    }
  }

  saveCustomClusters() {
    localStorage.setItem('kafka_custom_clusters', JSON.stringify(this.customClusters));
  }

  onClusterSelected(cluster: any) {
    this.selectedCluster = cluster;
    this.loadMockConnectors();
    this.loadTopicsForCluster();
    this.loadMockBrokers();
  }

  loadTopicsForCluster() {
    const clusterId = this.selectedCluster?.id;
    const bootstrapServers = this.selectedCluster?.configuration?.bootstrapServers;
    const securityProtocol = this.selectedCluster?.configuration?.security?.protocol || 'PLAINTEXT';

    if (!clusterId || !bootstrapServers) {
      this.topics = [];
      return;
    }

    this.isLoading = true;

    // Call real backend API to list topics
    this.kafkaManagementService.listTopics(clusterId, bootstrapServers, securityProtocol).subscribe({
      next: (topics) => {
        this.topics = topics;
        this.isLoading = false;
        console.log(`Loaded ${topics.length} topics from Kafka`);
      },
      error: (error) => {
        console.error('Failed to load topics:', error);
        this.snackBarService.error(`Failed to load topics: ${error.error?.error || error.message}`);
        this.topics = [];
        this.isLoading = false;
      },
    });
  }

  backToClusterSelection() {
    this.selectedCluster = null;
  }

  loadClusters() {
    this.clusterService.list('', undefined, 1, 100).subscribe({
      next: (result) => {
        // Only use clusters from API if they exist
        if (result.data && result.data.length > 0) {
          this.clusters = result.data;
        } else {
          this.clusters = [];
        }
        // Merge with custom clusters
        this.clusters = [...this.clusters, ...this.customClusters];
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading clusters:', error);
        // Start with empty clusters on error
        this.clusters = [...this.customClusters];
        this.isLoading = false;
      },
    });
  }

  openConfigureClusterDialog() {
    const dialogRef = this.dialog.open(AddClusterDialogComponent, {
      width: '600px',
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((result: ClusterConfig) => {
      if (result) {
        // Create cluster object
        const newCluster = {
          id: 'custom-' + Date.now(),
          name: result.name,
          configuration: {
            bootstrapServers: result.bootstrapServers,
            security: {
              protocol: result.securityProtocol,
            },
          },
          isCustom: true,
        };

        // Add to custom clusters
        this.customClusters.push(newCluster);
        this.saveCustomClusters();

        // Reload clusters
        this.clusters = [...this.clusters, newCluster];

        // Show success message
        console.log('Cluster configured successfully:', result);

        // Automatically select the new cluster
        setTimeout(() => {
          this.onClusterSelected(newCluster);
        }, 500);
      }
    });
  }

  deleteCluster(cluster: any, event: Event) {
    event.stopPropagation(); // Prevent card click

    // Confirm deletion
    if (confirm(`Are you sure you want to remove "${cluster.name}"?`)) {
      // Remove from custom clusters
      this.customClusters = this.customClusters.filter((c) => c.id !== cluster.id);
      this.saveCustomClusters();

      // Reload clusters
      this.clusters = this.clusters.filter((c) => c.id !== cluster.id);

      console.log('Cluster removed:', cluster.name);
    }
  }

  loadMockConnectors() {
    // Mock data for demo - realistic Kafka Connect connectors
    const clusterName = this.selectedCluster?.name || 'Local Kafka';
    this.connectors = [
      {
        name: 'mysql-source-connector',
        type: 'Source',
        status: 'Running',
        cluster: clusterName,
        tasks: '4/4',
        lastUpdate: '2 mins ago',
        class: 'io.debezium.connector.mysql.MySqlConnector',
        topics: ['mysql.customers', 'mysql.orders', 'mysql.products'],
      },
      {
        name: 'elasticsearch-sink',
        type: 'Sink',
        status: 'Running',
        cluster: clusterName,
        tasks: '2/2',
        lastUpdate: '5 mins ago',
        class: 'io.confluent.connect.elasticsearch.ElasticsearchSinkConnector',
        topics: ['search.index'],
      },
      {
        name: 'postgres-cdc-source',
        type: 'Source',
        status: 'Failed',
        cluster: clusterName,
        tasks: '2/4',
        lastUpdate: '1 hour ago',
        class: 'io.debezium.connector.postgresql.PostgresConnector',
        topics: ['postgres.users', 'postgres.transactions'],
      },
      {
        name: 's3-sink-connector',
        type: 'Sink',
        status: 'Paused',
        cluster: clusterName,
        tasks: '0/3',
        lastUpdate: '3 hours ago',
        class: 'io.confluent.connect.s3.S3SinkConnector',
        topics: ['logs.archive'],
      },
      {
        name: 'mongodb-source',
        type: 'Source',
        status: 'Running',
        cluster: clusterName,
        tasks: '1/1',
        lastUpdate: '10 mins ago',
        class: 'com.mongodb.kafka.connect.MongoSourceConnector',
        topics: ['mongo.events'],
      },
      {
        name: 'jdbc-sink-postgres',
        type: 'Sink',
        status: 'Running',
        cluster: clusterName,
        tasks: '3/3',
        lastUpdate: '15 mins ago',
        class: 'io.confluent.connect.jdbc.JdbcSinkConnector',
        topics: ['analytics.data'],
      },
    ];
  }

  openCreateTopicDialog() {
    const dialogRef = this.dialog.open(AddTopicDialogComponent, {
      width: '600px',
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((result: TopicConfig) => {
      if (result) {
        const clusterId = this.selectedCluster?.id;
        const bootstrapServers = this.selectedCluster?.configuration?.bootstrapServers;
        const securityProtocol = this.selectedCluster?.configuration?.security?.protocol || 'PLAINTEXT';

        if (!clusterId || !bootstrapServers) {
          this.snackBarService.error('Cluster information not available');
          return;
        }

        // Call real backend API to create topic
        this.kafkaManagementService
          .createTopic(clusterId, bootstrapServers, securityProtocol, {
            name: result.name,
            partitions: result.partitions,
            replicas: result.replicas,
            retentionMs: result.retentionMs,
            cleanupPolicy: result.cleanupPolicy,
          })
          .subscribe({
            next: (newTopic) => {
              this.snackBarService.success(`Topic "${result.name}" created successfully`);
              // Reload topics to show the new one
              this.loadTopicsForCluster();
            },
            error: (error) => {
              console.error('Failed to create topic:', error);
              this.snackBarService.error(`Failed to create topic: ${error.error?.error || error.message}`);
            },
          });
      }
    });
  }

  deleteTopic(topic: TopicData) {
    if (confirm(`Are you sure you want to delete topic "${topic.name}"?`)) {
      const clusterId = this.selectedCluster?.id;
      const bootstrapServers = this.selectedCluster?.configuration?.bootstrapServers;
      const securityProtocol = this.selectedCluster?.configuration?.security?.protocol || 'PLAINTEXT';

      if (!clusterId || !bootstrapServers) {
        this.snackBarService.error('Cluster information not available');
        return;
      }

      // Call real backend API to delete topic
      this.kafkaManagementService.deleteTopic(clusterId, bootstrapServers, securityProtocol, topic.name).subscribe({
        next: () => {
          this.snackBarService.success(`Topic "${topic.name}" deleted successfully`);
          // Reload topics to remove the deleted one
          this.loadTopicsForCluster();
        },
        error: (error) => {
          console.error('Failed to delete topic:', error);
          this.snackBarService.error(`Failed to delete topic: ${error.error?.error || error.message}`);
        },
      });
    }
  }

  loadMockBrokers() {
    // Mock brokers for localhost:9091
    this.brokers = [
      { id: 1, host: 'localhost', port: 9091, status: 'online' },
    ];
  }

  get filteredConnectors() {
    let filtered = this.connectors;
    if (this.searchTerm) {
      filtered = filtered.filter((c) =>
        c.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        c.class.toLowerCase().includes(this.searchTerm.toLowerCase()),
      );
    }
    return filtered;
  }

  get filteredTopics() {
    let filtered = this.topics;
    if (this.topicSearchTerm) {
      filtered = filtered.filter((t) => t.name.toLowerCase().includes(this.topicSearchTerm.toLowerCase()));
    }
    return filtered;
  }

  get runningCount(): number {
    return this.filteredConnectors.filter((c) => c.status === 'Running').length;
  }

  get pausedCount(): number {
    return this.filteredConnectors.filter((c) => c.status === 'Paused').length;
  }

  get failedCount(): number {
    return this.filteredConnectors.filter((c) => c.status === 'Failed').length;
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'Running':
        return 'check_circle';
      case 'Paused':
        return 'pause_circle';
      case 'Failed':
        return 'error';
      default:
        return 'help';
    }
  }

  getTypeIcon(type: string): string {
    return type === 'Source' ? 'download' : 'upload';
  }

  pauseConnector(connector: ConnectorData) {
    console.log('Pause connector:', connector.name);
  }

  resumeConnector(connector: ConnectorData) {
    console.log('Resume connector:', connector.name);
  }

  restartConnector(connector: ConnectorData) {
    console.log('Restart connector:', connector.name);
  }

  deleteConnector(connector: ConnectorData) {
    console.log('Delete connector:', connector.name);
  }

  viewConnectorDetails(connector: ConnectorData) {
    console.log('View connector details:', connector.name);
  }
}
