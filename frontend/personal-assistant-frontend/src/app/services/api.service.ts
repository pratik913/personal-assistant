import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  testBackend(): Observable<any> {
    return this.http.get(`${this.baseUrl}/health`);
  }
}